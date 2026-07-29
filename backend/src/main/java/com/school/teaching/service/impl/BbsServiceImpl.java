package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.BbsService;
import com.school.teaching.service.CreditService;
import com.school.teaching.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BbsServiceImpl implements BbsService {

    /** 基础敏感词正则（校园场景：欺凌、不当言论、联系方式等） */
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
        "傻[逼bB比]|[fF][uU][cC][kK]|[sS][hH][iI][tT]|sb|SB|尼玛|你妈|操你|草你|艹|日你|滚蛋|去死|废物|"
        + "微信|QQ|qq|手机号|电话|加我|私聊|面基|约架|打架|弄死|干死|砍死|"
        + "赌博|博彩|彩票|代考|代写|作弊|答案|卖|买|价格|多少钱|"
        + "pin\\s*yin|porn|色情|裸|黄片|约炮|一夜情",
        Pattern.CASE_INSENSITIVE);

    @Autowired private BbsCategoryMapper bbsCategoryMapper;
    @Autowired private BbsPostMapper bbsPostMapper;
    @Autowired private BbsReplyMapper bbsReplyMapper;
    @Autowired private BbsLikeMapper bbsLikeMapper;
    @Autowired private BbsBookmarkMapper bbsBookmarkMapper;
    @Autowired private BbsMutedUserMapper bbsMutedUserMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private NotificationMapper notificationMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private ClassesMapper classesMapper;
    @Autowired private CreditTransactionMapper creditTransactionMapper;
    @Autowired private CreditShopItemMapper creditShopItemMapper;
    @Autowired private CreditRuleMapper creditRuleMapper;
    @Autowired private CreditService creditService;
    @Autowired private NotificationService notificationService;

    @Override
    @Cacheable("bbsCategories")
    public List<BbsCategory> getCategories() {
        return bbsCategoryMapper.selectList(
            new LambdaQueryWrapper<BbsCategory>()
                .eq(BbsCategory::getStatus, 1)
                .orderByAsc(BbsCategory::getSortOrder));
    }

    @Override
    public Map<String, Object> getPosts(Long categoryId, String keyword, int page, int pageSize, String sort, Boolean highlightOnly, Long currentUserId) {
        LambdaQueryWrapper<BbsPost> w = new LambdaQueryWrapper<>();
        w.eq(BbsPost::getStatus, "normal");
        if (highlightOnly != null && highlightOnly) {
            w.eq(BbsPost::getIsHighlighted, 1);
        }
        if (categoryId != null && categoryId > 0) {
            w.eq(BbsPost::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            w.and(ww -> ww.like(BbsPost::getTitle, keyword.trim())
                .or().like(BbsPost::getContent, keyword.trim()));
        }
        // MyBatis-Plus 标准分页（参数化查询，无SQL注入风险）
        w.orderByDesc(BbsPost::getIsSticky);
        if ("hottest".equals(sort)) {
            w.orderByDesc(BbsPost::getReplyCount);
        } else {
            w.orderByDesc(BbsPost::getLastReplyTime); // 默认按最新回复
        }
        Page<BbsPost> mpPage = new Page<>(page, pageSize);
        mpPage = bbsPostMapper.selectPage(mpPage, w);
        List<BbsPost> pageList = mpPage.getRecords();
        long total = mpPage.getTotal();

        List<Map<String, Object>> records = batchPostToMaps(pageList, currentUserId);
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    @Override
    public Map<String, Object> getPostDetail(Long postId, Long currentUserId) {
        BbsPost post = bbsPostMapper.selectById(postId);
        if (post == null) return null;
        Map<String, Object> detail = postToMap(post, currentUserId);
        detail.put("replies", getReplies(postId, currentUserId, 1, 20).get("records"));
        // 相关推荐：同板块帖子（排除当前帖），按回复数排序，最多4篇
        Page<BbsPost> relatedPage = new Page<>(1, 4);
        relatedPage = bbsPostMapper.selectPage(relatedPage,
            new LambdaQueryWrapper<BbsPost>()
                .eq(BbsPost::getCategoryId, post.getCategoryId())
                .eq(BbsPost::getStatus, "normal")
                .ne(BbsPost::getId, postId)
                .orderByDesc(BbsPost::getReplyCount));
        List<BbsPost> related = relatedPage.getRecords();
        detail.put("relatedPosts", batchPostToMaps(related, currentUserId));
        return detail;
    }

    @Override
    @Transactional
    public BbsPost createPost(BbsPost post) {
        Long authorId = post.getAuthorId();
        // 禁言检查
        if (isMuted(authorId)) throw new BusinessException(403, "您已被禁言，无法发帖");
        // 频率检查：60秒内不得重复发帖
        if (authorId != null) {
            Long recentCount = bbsPostMapper.selectCount(new LambdaQueryWrapper<BbsPost>()
                .eq(BbsPost::getAuthorId, authorId)
                .gt(BbsPost::getCreateTime, LocalDateTime.now().minusSeconds(60)));
            if (recentCount > 0) throw new BusinessException(429, "发帖过于频繁，请60秒后再试");
        }
        // 内容校验
        String content = post.getContent();
        if (content == null || content.trim().length() < 10)
            throw new BusinessException(400, "帖子内容至少10个字符");
        if (post.getTitle() == null || post.getTitle().trim().length() < 4)
            throw new BusinessException(400, "标题至少4个字符");
        // 纯数字/纯符号灌水检测
        if (content.trim().matches("^[\\d\\s]+$") || post.getTitle().trim().matches("^[\\d\\s]+$"))
            throw new BusinessException(400, "标题或内容不能为纯数字");
        // 敏感词检测
        String checkText = (post.getTitle() != null ? post.getTitle() : "") + " " + content;
        if (SENSITIVE_PATTERN.matcher(checkText).find()) {
            throw new BusinessException(400, "内容包含不当词汇，请修改后重试");
        }
        // 重复内容检测（6小时内相同内容）
        if (authorId != null) {
            Long dupCount = bbsPostMapper.selectCount(new LambdaQueryWrapper<BbsPost>()
                .eq(BbsPost::getAuthorId, authorId)
                .eq(BbsPost::getContent, content.trim())
                .gt(BbsPost::getCreateTime, LocalDateTime.now().minusHours(6)));
            if (dupCount > 0) throw new BusinessException(400, "请勿重复发布相同内容");
        }
        post.setStatus("normal");
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setReplyCount(0);
        post.setCreateTime(LocalDateTime.now());
        post.setLastReplyTime(post.getCreateTime());
        bbsPostMapper.insert(post);
        updateCategoryPostCount(post.getCategoryId());
        // 积分：内容≥30字得2分，≥10字得1分
        int credit = content.trim().length() >= 30 ? 2 : 1;
        awardBbsCredit(authorId, credit, "post", post.getId(), "发布帖子", 1);
        return post;
    }

    @Override
    @Transactional
    public BbsPost updatePost(BbsPost post) {
        // 敏感词检测
        String checkText = (post.getTitle() != null ? post.getTitle() : "") + " " +
                          (post.getContent() != null ? post.getContent() : "");
        if (SENSITIVE_PATTERN.matcher(checkText).find()) {
            throw new BusinessException(400, "内容包含不当词汇，请修改后重试");
        }
        BbsPost existing = bbsPostMapper.selectById(post.getId());
        if (existing == null) throw new BusinessException(404, "帖子不存在");
        // 只允许作者编辑
        if (!existing.getAuthorId().equals(post.getAuthorId())) {
            throw new BusinessException(403, "无权编辑他人的帖子");
        }
        existing.setTitle(post.getTitle());
        existing.setContent(post.getContent());
        existing.setImages(post.getImages());
        existing.setCategoryId(post.getCategoryId());
        bbsPostMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long userId, String role) {
        BbsPost post = bbsPostMapper.selectById(postId);
        if (post == null) throw new BusinessException(404, "帖子不存在");
        boolean isTeacher = "SUPER_ADMIN".equals(role) || "TEACHER".equals(role) || "HEAD_TEACHER".equals(role) || "ADMIN".equals(role);
        if (!post.getAuthorId().equals(userId) && !isTeacher) {
            throw new BusinessException(403, "无权删除该帖子");
        }
        // 软删除
        post.setStatus("deleted");
        bbsPostMapper.updateById(post);
        updateCategoryPostCount(post.getCategoryId());
    }

    @Override
    @Transactional
    public void toggleSticky(Long postId) {
        BbsPost post = bbsPostMapper.selectById(postId);
        if (post == null) throw new BusinessException(404, "帖子不存在");
        post.setIsSticky(post.getIsSticky() == 1 ? 0 : 1);
        bbsPostMapper.updateById(post);
        // 通知帖子作者
        if (post.getIsSticky() == 1) {
            notificationService.notify(post.getAuthorId(), "bbs_sticky",
                "📌 帖子已置顶", "你的帖子《" + post.getTitle() + "》已被教师置顶", postId);
        }
    }

    @Override
    @Transactional
    public void useStickyCoupon(Long postId, Long userId) {
        BbsPost post = validatePostOwnership(postId, userId);
        Student student = findStudentByUserId(userId);
        Long stickyItemId = findStickyCouponItemId();
        checkAvailableCoupons(student.getId(), stickyItemId);
        applyStickyAndRecordUsage(post, student, stickyItemId);
    }

    private BbsPost validatePostOwnership(Long postId, Long userId) {
        BbsPost post = bbsPostMapper.selectById(postId);
        if (post == null) throw new BusinessException(404, "帖子不存在");
        if (!post.getAuthorId().equals(userId)) throw new BusinessException(403, "只能置顶自己的帖子");
        return post;
    }

    private Student findStudentByUserId(Long userId) {
        Student student = studentMapper.selectOne(
            new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
        if (student == null) throw new BusinessException(404, "学生信息不存在");
        return student;
    }

    private Long findStickyCouponItemId() {
        CreditShopItem stickyItem = creditShopItemMapper.selectOne(
            new LambdaQueryWrapper<CreditShopItem>()
                .eq(CreditShopItem::getItemName, "帖子置顶券")
                .eq(CreditShopItem::getStatus, 1));
        if (stickyItem == null) throw new BusinessException(404, "置顶券商品不存在，请联系管理员");
        return stickyItem.getId();
    }

    private void checkAvailableCoupons(Long studentId, Long stickyItemId) {
        LambdaQueryWrapper<CreditTransaction> couponCheck = new LambdaQueryWrapper<>();
        couponCheck.eq(CreditTransaction::getStudentId, studentId)
            .eq(CreditTransaction::getSourceType, "redeem")
            .eq(CreditTransaction::getSourceId, stickyItemId);
        List<CreditTransaction> coupons = creditTransactionMapper.selectList(couponCheck);

        LambdaQueryWrapper<CreditTransaction> usedCheck = new LambdaQueryWrapper<>();
        usedCheck.eq(CreditTransaction::getStudentId, studentId)
            .eq(CreditTransaction::getSourceType, "redeem_use")
            .eq(CreditTransaction::getSourceId, stickyItemId);
        long usedCount = creditTransactionMapper.selectCount(usedCheck);
        if (coupons.size() - usedCount <= 0) {
            throw new BusinessException(400, "没有可用的置顶券，请先在积分商城兑换");
        }
    }

    private void applyStickyAndRecordUsage(BbsPost post, Student student, Long stickyItemId) {
        post.setIsSticky(1);
        bbsPostMapper.updateById(post);

        CreditTransaction useRecord = new CreditTransaction();
        useRecord.setStudentId(student.getId());
        useRecord.setTransactionType("consume");
        useRecord.setCreditAmount(50);
        useRecord.setBalanceAfter(student.getTotalCredits());
        useRecord.setSourceType("redeem_use");
        useRecord.setSourceId(stickyItemId);
        useRecord.setDescription("使用帖子置顶券 - " + post.getTitle());
        useRecord.setCreateTime(java.time.LocalDateTime.now());
        creditTransactionMapper.insert(useRecord);
    }

    @Override
    @Transactional
    public void toggleHighlight(Long postId) {
        BbsPost post = bbsPostMapper.selectById(postId);
        if (post == null) throw new BusinessException(404, "帖子不存在");
        boolean wasHighlighted = post.getIsHighlighted() == 1;
        post.setIsHighlighted(wasHighlighted ? 0 : 1);
        bbsPostMapper.updateById(post);
        // 设为精华时奖励10积分+通知
        if (!wasHighlighted) {
            awardBbsCredit(post.getAuthorId(), 10, "highlight", postId, "帖子被设为精华", 0);
            notificationService.notify(post.getAuthorId(), "bbs_highlight",
                "🌟 帖子被加精", "你的帖子《" + post.getTitle() + "》已被设为精华帖，获得10积分奖励", postId);
        }
    }

    @Override
    @Transactional
    public void incrementViewCount(Long postId) {
        bbsPostMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<BbsPost>()
                .eq(BbsPost::getId, postId)
                .setSql("view_count = view_count + 1"));
    }

    @Override
    public Map<String, Object> getReplies(Long postId, Long currentUserId, int page, int pageSize) {
        LambdaQueryWrapper<BbsReply> qw = new LambdaQueryWrapper<BbsReply>()
            .eq(BbsReply::getPostId, postId)
            .eq(BbsReply::getStatus, "normal")
            .orderByAsc(BbsReply::getCreateTime);
        Page<BbsReply> mpPage = new Page<>(page, pageSize);
        mpPage = bbsReplyMapper.selectPage(mpPage, qw);
        List<BbsReply> replies = mpPage.getRecords();

        Map<Long, User> userMap = loadReplyAuthors(replies);
        Map<Long, Student> studentMap = loadReplyStudents(replies);
        Map<Long, String> classNameMap = loadReplyClassNames(studentMap);
        Set<Long> likedReplyIds = loadLikedReplyIds(replies, currentUserId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (BbsReply r : replies) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("postId", r.getPostId());
            item.put("parentId", r.getParentId());
            item.put("content", r.getContent());
            item.put("authorId", r.getAuthorId());
            item.put("likeCount", r.getLikeCount());
            item.put("createTime", r.getCreateTime());
            User author = userMap.get(r.getAuthorId());
            item.put("authorName", author != null ? author.getRealName() : "未知");
            Student st = studentMap.get(r.getAuthorId());
            item.put("authorStatus", st != null ? st.getStatus() : null);
            item.put("authorClassName", st != null && st.getClassId() != null ? classNameMap.get(st.getClassId()) : null);
            item.put("liked", currentUserId != null && likedReplyIds.contains(r.getId()));
            result.add(item);
        }
        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("records", result);
        pageResult.put("total", mpPage.getTotal());
        pageResult.put("hasMore", mpPage.getCurrent() * mpPage.getSize() < mpPage.getTotal());
        return pageResult;
    }

    private Map<Long, User> loadReplyAuthors(List<BbsReply> replies) {
        Set<Long> authorIds = replies.stream().map(BbsReply::getAuthorId).filter(Objects::nonNull).collect(Collectors.toSet());
        return authorIds.isEmpty() ? Collections.emptyMap() :
            userMapper.selectBatchIds(authorIds).stream().collect(Collectors.toMap(User::getId, u -> u));
    }

    private Map<Long, Student> loadReplyStudents(List<BbsReply> replies) {
        Set<Long> authorIds = replies.stream().map(BbsReply::getAuthorId).filter(Objects::nonNull).collect(Collectors.toSet());
        return authorIds.isEmpty() ? Collections.emptyMap() :
            studentMapper.selectList(new LambdaQueryWrapper<Student>().in(Student::getUserId, authorIds))
                .stream().collect(Collectors.toMap(Student::getUserId, s -> s));
    }

    private Map<Long, String> loadReplyClassNames(Map<Long, Student> studentMap) {
        Set<Long> classIds = studentMap.values().stream()
            .map(Student::getClassId).filter(Objects::nonNull).collect(Collectors.toSet());
        return classIds.isEmpty() ? Collections.emptyMap() :
            classesMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(Classes::getId, Classes::getClassName));
    }

    private Set<Long> loadLikedReplyIds(List<BbsReply> replies, Long currentUserId) {
        if (currentUserId == null) return Collections.emptySet();
        Set<Long> replyIds = replies.stream().map(BbsReply::getId).collect(Collectors.toSet());
        if (replyIds.isEmpty()) return Collections.emptySet();
        return bbsLikeMapper.selectList(
                new LambdaQueryWrapper<BbsLike>()
                    .select(BbsLike::getTargetId)
                    .eq(BbsLike::getUserId, currentUserId)
                    .eq(BbsLike::getTargetType, "reply")
                    .in(BbsLike::getTargetId, replyIds))
                .stream().map(BbsLike::getTargetId).collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public Map<String, Object> createReply(Long postId, Long parentId, String content, Long authorId, String authorName) {
        // 检查禁言
        if (isMuted(authorId)) throw new BusinessException(403, "您已被禁言");
        // 内容校验
        if (content == null || content.trim().length() < 5)
            throw new BusinessException(400, "回复内容至少5个字符");
        // 频率检查：15秒内不得重复回复
        Long recentReply = bbsReplyMapper.selectCount(new LambdaQueryWrapper<BbsReply>()
            .eq(BbsReply::getAuthorId, authorId)
            .gt(BbsReply::getCreateTime, LocalDateTime.now().minusSeconds(15)));
        if (recentReply > 0) throw new BusinessException(429, "回复过于频繁，请15秒后再试");
        // 敏感词检测
        if (SENSITIVE_PATTERN.matcher(content).find())
            throw new BusinessException(400, "内容包含不当词汇，请修改后重试");

        BbsPost post = bbsPostMapper.selectById(postId);
        if (post == null) throw new BusinessException(404, "帖子不存在");

        BbsReply reply = new BbsReply();
        reply.setPostId(postId);
        reply.setParentId(parentId);
        reply.setContent(content);
        reply.setAuthorId(authorId);
        reply.setLikeCount(0);
        reply.setStatus("normal");
        bbsReplyMapper.insert(reply);

        // 更新帖子回复数和最后回复时间
        post.setReplyCount((post.getReplyCount() == null ? 0 : post.getReplyCount()) + 1);
        post.setLastReplyTime(LocalDateTime.now());
        bbsPostMapper.updateById(post);

        // 解析 @提及 并创建通知
        parseMentions(content, postId, reply.getId(), authorId, authorName);

        // 回复奖励1积分（每日上限5次）
        awardBbsCredit(authorId, 1, "reply", reply.getId(), "回复帖子", 5);

        // 通知帖子作者（非自己回复时）
        if (!authorId.equals(post.getAuthorId())) {
            notificationService.notify(post.getAuthorId(), "bbs_reply",
                "💬 新回复", authorName + " 回复了你的帖子《" + post.getTitle() + "》", postId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", reply.getId());
        result.put("createTime", reply.getCreateTime());
        return result;
    }

    @Override
    @Transactional
    public void deleteReply(Long replyId, Long userId, String role) {
        BbsReply reply = bbsReplyMapper.selectById(replyId);
        if (reply == null) throw new BusinessException(404, "回复不存在");
        boolean isTeacher = "SUPER_ADMIN".equals(role) || "TEACHER".equals(role) || "HEAD_TEACHER".equals(role) || "ADMIN".equals(role);
        if (!reply.getAuthorId().equals(userId) && !isTeacher) {
            throw new BusinessException(403, "无权删除该回复");
        }
        reply.setStatus("deleted");
        bbsReplyMapper.updateById(reply);
        // 更新帖子回复数
        BbsPost post = bbsPostMapper.selectById(reply.getPostId());
        if (post != null) {
            long count = bbsReplyMapper.selectCount(
                new LambdaQueryWrapper<BbsReply>()
                    .eq(BbsReply::getPostId, reply.getPostId())
                    .eq(BbsReply::getStatus, "normal"));
            post.setReplyCount((int) count);
            bbsPostMapper.updateById(post);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> toggleLike(Long userId, Long targetId, String targetType) {
        LambdaQueryWrapper<BbsLike> w = new LambdaQueryWrapper<BbsLike>()
            .eq(BbsLike::getUserId, userId)
            .eq(BbsLike::getTargetId, targetId)
            .eq(BbsLike::getTargetType, targetType);
        BbsLike existing = bbsLikeMapper.selectOne(w);
        boolean liked;
        if (existing != null) {
            bbsLikeMapper.deleteById(existing.getId());
            liked = false;
        } else {
            BbsLike like = new BbsLike();
            like.setUserId(userId);
            like.setTargetId(targetId);
            like.setTargetType(targetType);
            bbsLikeMapper.insert(like);
            liked = true;
            handleLikeNotification(userId, targetId, targetType);
        }
        int count = updateLikeCount(targetId, targetType, liked);
        return Map.of("liked", liked, "count", count);
    }

    private void handleLikeNotification(Long userId, Long targetId, String targetType) {
        Long authorId = "post".equals(targetType)
            ? Optional.ofNullable(bbsPostMapper.selectById(targetId)).map(BbsPost::getAuthorId).orElse(null)
            : Optional.ofNullable(bbsReplyMapper.selectById(targetId)).map(BbsReply::getAuthorId).orElse(null);
        if (authorId == null || authorId.equals(userId)) return;
        awardBbsCredit(authorId, 1, "like", targetId, "帖子被点赞", 10);
        User liker = userMapper.selectById(userId);
        String likerName = liker != null ? liker.getRealName() : "有人";
        String targetLabel = "post".equals(targetType) ? "帖子" : "回复";
        notificationService.notify(authorId, "bbs_like",
            "收到点赞", likerName + " 赞了你的" + targetLabel, targetId);
    }

    private int updateLikeCount(Long targetId, String targetType, boolean increment) {
        String sql = increment
            ? "like_count = COALESCE(like_count, 0) + 1"
            : "like_count = GREATEST(COALESCE(like_count, 0) - 1, 0)";
        if ("post".equals(targetType)) {
            bbsPostMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<BbsPost>()
                    .eq(BbsPost::getId, targetId)
                    .setSql(sql));
        } else {
            bbsReplyMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<BbsReply>()
                    .eq(BbsReply::getId, targetId)
                    .setSql(sql));
        }
        // 返回最新计数
        if ("post".equals(targetType)) {
            BbsPost post = bbsPostMapper.selectById(targetId);
            return post != null && post.getLikeCount() != null ? post.getLikeCount() : 0;
        } else {
            BbsReply reply = bbsReplyMapper.selectById(targetId);
            return reply != null && reply.getLikeCount() != null ? reply.getLikeCount() : 0;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> toggleBookmark(Long userId, Long postId) {
        LambdaQueryWrapper<BbsBookmark> w = new LambdaQueryWrapper<BbsBookmark>()
            .eq(BbsBookmark::getUserId, userId)
            .eq(BbsBookmark::getPostId, postId);
        BbsBookmark existing = bbsBookmarkMapper.selectOne(w);
        boolean bookmarked;
        if (existing != null) {
            bbsBookmarkMapper.deleteById(existing.getId());
            bookmarked = false;
        } else {
            BbsBookmark bm = new BbsBookmark();
            bm.setUserId(userId);
            bm.setPostId(postId);
            bbsBookmarkMapper.insert(bm);
            bookmarked = true;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("bookmarked", bookmarked);
        return result;
    }

    @Override
    public List<Map<String, Object>> getMyPosts(Long userId) {
        List<BbsPost> posts = bbsPostMapper.selectList(
            new LambdaQueryWrapper<BbsPost>()
                .eq(BbsPost::getAuthorId, userId)
                .orderByDesc(BbsPost::getCreateTime));
        return batchPostToMaps(posts, userId);
    }

    @Override
    public List<Map<String, Object>> getMyBookmarks(Long userId) {
        List<BbsBookmark> bms = bbsBookmarkMapper.selectList(
            new LambdaQueryWrapper<BbsBookmark>()
                .eq(BbsBookmark::getUserId, userId)
                .orderByDesc(BbsBookmark::getCreateTime));
        // 批量加载书签对应的帖子
        Set<Long> postIds = bms.stream().map(BbsBookmark::getPostId).collect(Collectors.toSet());
        Map<Long, BbsPost> postMap = postIds.isEmpty() ? Collections.emptyMap() :
            bbsPostMapper.selectBatchIds(postIds).stream()
                .filter(p -> "normal".equals(p.getStatus()))
                .collect(Collectors.toMap(BbsPost::getId, p -> p));
        List<BbsPost> validPosts = bms.stream()
            .map(bm -> postMap.get(bm.getPostId())).filter(Objects::nonNull)
            .collect(Collectors.toList());
        List<Map<String, Object>> maps = batchPostToMaps(validPosts, userId);
        // 填充书签特有字段
        Map<Long, BbsBookmark> bmByPostId = bms.stream()
            .collect(Collectors.toMap(BbsBookmark::getPostId, b -> b, (a, b) -> a));
        for (Map<String, Object> item : maps) {
            Long pid = (Long) item.get("id");
            BbsBookmark bm = bmByPostId.get(pid);
            if (bm != null) {
                item.put("bookmarkId", bm.getId());
                item.put("bookmarkTime", bm.getCreateTime());
            }
        }
        return maps;
    }

    @Override
    @Transactional
    public void muteUser(Long userId, Long mutedBy, String reason, Integer durationDays) {
        BbsMutedUser existing = bbsMutedUserMapper.selectOne(
            new LambdaQueryWrapper<BbsMutedUser>().eq(BbsMutedUser::getUserId, userId));
        if (existing != null) {
            // 已有禁言记录但已过期：删除旧记录
            if (existing.getExpireTime() != null && existing.getExpireTime().isBefore(LocalDateTime.now())) {
                bbsMutedUserMapper.deleteById(existing.getId());
            } else {
                return; // 有效禁言中
            }
        }
        BbsMutedUser mu = new BbsMutedUser();
        mu.setUserId(userId);
        mu.setMutedBy(mutedBy);
        mu.setReason(reason);
        if (durationDays != null && durationDays > 0) {
            mu.setExpireTime(LocalDateTime.now().plusDays(durationDays));
        }
        bbsMutedUserMapper.insert(mu);
        String durationText = durationDays != null && durationDays > 0 ? "，为期" + durationDays + "天" : "，永久有效";
        notificationService.notify(userId, "bbs_muted",
            "禁言通知", "你已被禁言" + durationText + (reason != null && !reason.isEmpty() ? "，原因：" + reason : ""), null);
    }

    @Override
    @Transactional
    public void unmuteUser(Long userId) {
        bbsMutedUserMapper.delete(
            new LambdaQueryWrapper<BbsMutedUser>().eq(BbsMutedUser::getUserId, userId));
    }

    @Override
    public boolean isMuted(Long userId) {
        BbsMutedUser mu = bbsMutedUserMapper.selectOne(
            new LambdaQueryWrapper<BbsMutedUser>().eq(BbsMutedUser::getUserId, userId));
        if (mu == null) return false;
        // 有期限且已过期 → 自动解禁
        if (mu.getExpireTime() != null && mu.getExpireTime().isBefore(LocalDateTime.now())) {
            bbsMutedUserMapper.deleteById(mu.getId());
            return false;
        }
        return true;
    }

    // ==================== 私有工具方法 ====================

    /** 单帖映射（用于 getPostDetail），委托批量版本 */
    private Map<String, Object> postToMap(BbsPost post, Long currentUserId) {
        return batchPostToMaps(Collections.singletonList(post), currentUserId).get(0);
    }

    /** 批量帖映射：一次查询加载所有关联数据，消除 N+1 */
    private record PostBatchData(
        Map<Long, BbsCategory> categoryMap,
        Map<Long, User> userMap,
        Map<Long, Student> studentMapByUserId,
        Map<Long, Classes> classMap,
        Map<Long, Long> certCountMap,
        Set<Long> likedPostIds,
        Set<Long> bookmarkedPostIds
    ) {}

    private List<Map<String, Object>> batchPostToMaps(List<BbsPost> posts, Long currentUserId) {
        if (posts.isEmpty()) return Collections.emptyList();
        PostBatchData data = loadPostBatchData(posts, currentUserId);
        return posts.stream().map(p -> buildPostMap(p, currentUserId, data)).collect(Collectors.toList());
    }

    private PostBatchData loadPostBatchData(List<BbsPost> posts, Long currentUserId) {
        Set<Long> categoryIds = new HashSet<>();
        Set<Long> authorIds = new HashSet<>();
        Set<Long> postIds = new HashSet<>();
        for (BbsPost p : posts) {
            if (p.getCategoryId() != null) categoryIds.add(p.getCategoryId());
            if (p.getAuthorId() != null) authorIds.add(p.getAuthorId());
            postIds.add(p.getId());
        }

        Map<Long, BbsCategory> categoryMap = categoryIds.isEmpty() ? Collections.emptyMap() :
            bbsCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(BbsCategory::getId, c -> c));
        Map<Long, User> userMap = authorIds.isEmpty() ? Collections.emptyMap() :
            userMapper.selectBatchIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Student> studentMapByUserId = authorIds.isEmpty() ? Collections.emptyMap() :
            studentMapper.selectList(new LambdaQueryWrapper<Student>().in(Student::getUserId, authorIds))
                .stream().collect(Collectors.toMap(Student::getUserId, s -> s));
        Set<Long> classIds = studentMapByUserId.values().stream()
            .map(Student::getClassId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Classes> classMap = classIds.isEmpty() ? Collections.emptyMap() :
            classesMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(Classes::getId, c -> c));

        Map<Long, Long> certCountMap = loadCertCounts(studentMapByUserId, userMap);

        Set<Long> likedPostIds = Collections.emptySet();
        Set<Long> bookmarkedPostIds = Collections.emptySet();
        if (currentUserId != null && !postIds.isEmpty()) {
            likedPostIds = bbsLikeMapper.selectList(
                new LambdaQueryWrapper<BbsLike>()
                    .select(BbsLike::getTargetId)
                    .eq(BbsLike::getUserId, currentUserId)
                    .eq(BbsLike::getTargetType, "post")
                    .in(BbsLike::getTargetId, postIds))
                .stream().map(BbsLike::getTargetId).collect(Collectors.toSet());
            bookmarkedPostIds = bbsBookmarkMapper.selectList(
                new LambdaQueryWrapper<BbsBookmark>()
                    .select(BbsBookmark::getPostId)
                    .eq(BbsBookmark::getUserId, currentUserId)
                    .in(BbsBookmark::getPostId, postIds))
                .stream().map(BbsBookmark::getPostId).collect(Collectors.toSet());
        }

        return new PostBatchData(categoryMap, userMap, studentMapByUserId, classMap,
            certCountMap, likedPostIds, bookmarkedPostIds);
    }

    private Map<Long, Long> loadCertCounts(Map<Long, Student> studentMapByUserId, Map<Long, User> userMap) {
        Set<Long> studentIds = studentMapByUserId.values().stream()
            .filter(s -> {
                User u = userMap.get(s.getUserId());
                return u != null && (u.getRoleId() & 4) != 0;
            })
            .map(Student::getId).collect(Collectors.toSet());
        Map<Long, Long> certCountMap = new HashMap<>();
        if (!studentIds.isEmpty()) {
            List<Map<String, Object>> certRows = creditTransactionMapper.selectMaps(
                new LambdaQueryWrapper<CreditTransaction>()
                    .select(CreditTransaction::getStudentId)
                    .in(CreditTransaction::getStudentId, studentIds)
                    .eq(CreditTransaction::getSourceType, "redeem")
                    .like(CreditTransaction::getDescription, "荣誉证书"));
            for (Map<String, Object> row : certRows) {
                Long sid = (Long) row.get("student_id");
                certCountMap.merge(sid, 1L, Long::sum);
            }
        }
        return certCountMap;
    }

    private Map<String, Object> buildPostMap(BbsPost p, Long currentUserId, PostBatchData data) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", p.getId());
        item.put("categoryId", p.getCategoryId());
        item.put("title", p.getTitle());
        item.put("content", p.getContent());
        item.put("images", p.getImages());
        item.put("authorId", p.getAuthorId());
        item.put("isSticky", p.getIsSticky());
        item.put("isHighlighted", p.getIsHighlighted());
        item.put("viewCount", p.getViewCount());
        item.put("likeCount", p.getLikeCount());
        item.put("replyCount", p.getReplyCount());
        item.put("lastReplyTime", p.getLastReplyTime());
        item.put("createTime", p.getCreateTime());

        BbsCategory cat = data.categoryMap().get(p.getCategoryId());
        item.put("categoryName", cat != null ? cat.getName() : "");
        item.put("categoryIcon", cat != null ? cat.getIcon() : "");
        item.put("categoryDescription", cat != null ? cat.getDescription() : "");

        User author = data.userMap().get(p.getAuthorId());
        item.put("authorName", author != null ? author.getRealName() : "未知");
        item.put("authorAvatar", author != null ? author.getAvatarUrl() : null);

        Student authorStudent = author != null ? data.studentMapByUserId().get(author.getId()) : null;
        item.put("authorStatus", authorStudent != null ? authorStudent.getStatus() : null);
        item.put("authorClassName", authorStudent != null && authorStudent.getClassId() != null
            ? Optional.ofNullable(data.classMap().get(authorStudent.getClassId())).map(Classes::getClassName).orElse(null)
            : null);

        String customTitle = null;
        int certCount = 0;
        if (author != null && (author.getRoleId() & 4) != 0 && authorStudent != null) {
            if (authorStudent.getCustomTitle() != null && authorStudent.getCustomTitleSetAt() != null
                && LocalDateTime.now().isBefore(authorStudent.getCustomTitleSetAt().plusDays(7))) {
                customTitle = authorStudent.getCustomTitle();
            }
            certCount = data.certCountMap().getOrDefault(authorStudent.getId(), 0L).intValue();
        }
        item.put("authorCustomTitle", customTitle);
        item.put("authorCertCount", certCount);

        item.put("liked", currentUserId != null && data.likedPostIds().contains(p.getId()));
        item.put("bookmarked", currentUserId != null && data.bookmarkedPostIds().contains(p.getId()));
        return item;
    }

    private void updateCategoryPostCount(Long categoryId) {
        long count = bbsPostMapper.selectCount(
            new LambdaQueryWrapper<BbsPost>()
                .eq(BbsPost::getCategoryId, categoryId)
                .eq(BbsPost::getStatus, "normal"));
        bbsCategoryMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<BbsCategory>()
                .eq(BbsCategory::getId, categoryId)
                .set(BbsCategory::getPostCount, (int) count));
    }

    /** 解析 @用户名 并创建通知 */
    private void parseMentions(String content, Long postId, Long replyId, Long fromUserId, String fromUserName) {
        if (content == null || content.isEmpty()) return;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("@(\\S+?)(?=\\s|$|\\p{P})").matcher(content);
        // 先收集所有@的用户名，批量查询
        Set<String> mentionedNames = new java.util.HashSet<>();
        while (m.find()) mentionedNames.add(m.group(1));
        if (mentionedNames.isEmpty()) return;
        Map<String, User> userMap = userMapper.selectList(
            new LambdaQueryWrapper<User>().in(User::getUsername, mentionedNames))
            .stream().collect(java.util.stream.Collectors.toMap(User::getUsername, u -> u));
        m.reset();
        while (m.find()) {
            String mentionedName = m.group(1);
            User mentionedUser = userMap.get(mentionedName);
            if (mentionedUser != null && !mentionedUser.getId().equals(fromUserId)) {
                // 创建通知 (简易实现：直接插入 notification 表)
                try {
                    // 使用 NotificationMapper 插入通知
                    Notification notif = new Notification();
                    notif.setUserId(mentionedUser.getId());
                    notif.setTitle("有人提到了你");
                    notif.setContent(fromUserName + " 在回复中提到了你");
                    notif.setType("bbs_mention");
                    notif.setRelatedId(postId);
                    notif.setIsRead(0);
                    notificationMapper.insert(notif);
                } catch (Exception e) {
                    log.warn("parseMentions: failed to create mention notification: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * BBS 行为积分奖励（仅限学生角色）
     */
    private void awardBbsCredit(Long userId, int amount, String sourceType, Long sourceId,
                                 String description, int dailyLimit) {
        if (userId == null) return;
        User user = userMapper.selectById(userId);
        if (user == null || user.getRoleId() == null || (user.getRoleId() & 4) == 0) return;

        Student student = studentMapper.selectOne(
            new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
        if (student == null) return;

        if (dailyLimit > 0) {
            long todayCount = creditTransactionMapper.selectCount(
                new LambdaQueryWrapper<CreditTransaction>()
                    .eq(CreditTransaction::getStudentId, student.getId())
                    .eq(CreditTransaction::getSourceType, sourceType)
                    .apply("DATE(create_time) = CURDATE()"));
            if (todayCount >= dailyLimit) return;
        }

        int oldCredits = student.getTotalCredits() != null ? student.getTotalCredits() : 0;
        student.setTotalCredits(oldCredits + amount);
        studentMapper.updateById(student);

        CreditTransaction ct = new CreditTransaction();
        ct.setStudentId(student.getId());
        ct.setTransactionType("earn");
        ct.setCreditAmount(amount);
        ct.setBalanceAfter(student.getTotalCredits());
        ct.setSourceType(sourceType);
        ct.setSourceId(sourceId);
        ct.setDescription(description + " +" + amount + "积分");
        ct.setCreateTime(java.time.LocalDateTime.now());
        creditTransactionMapper.insert(ct);
    }

    @Override @Transactional
    public Map<String, Object> createMoralPost(Map<String, Object> body, Long userId, List<Long> teachingClassIds) {
        String title = (String) body.getOrDefault("title", "德育行为表扬");
        String content = (String) body.get("content");
        Long praisedStudentId = Long.valueOf(body.get("praisedStudentId").toString());
        Long classId = Long.valueOf(body.get("classId").toString());
        Long categoryId = body.get("categoryId") != null ? Long.valueOf(body.get("categoryId").toString()) : findMoralCategoryId();

        Student praised = studentMapper.selectById(praisedStudentId);
        if (praised == null || !classId.equals(praised.getClassId()))
            throw new BusinessException(400, "该学生不属于指定班级");

        CreditRule moralRule = creditRuleMapper.selectOne(
            new LambdaQueryWrapper<CreditRule>().eq(CreditRule::getRuleCode, "MORAL_BEHAVIOR").eq(CreditRule::getStatus, 1));
        if (moralRule == null) throw new BusinessException(500, "德育积分规则未配置");
        int creditValue = moralRule.getCreditValue();

        BbsPost post = new BbsPost();
        post.setTitle(title); post.setContent(content); post.setAuthorId(userId); post.setCategoryId(categoryId);
        post.setIsMoralBehavior(1); post.setPraisedStudentId(praisedStudentId);
        post.setStatus("normal"); post.setViewCount(0); post.setLikeCount(0); post.setReplyCount(0);
        post.setCreateTime(LocalDateTime.now()); post.setLastReplyTime(post.getCreateTime());
        bbsPostMapper.insert(post);

        creditService.awardMoralCredit(praisedStudentId, creditValue, "德育表扬: " + title);

        User praisedUser = userMapper.selectById(praised.getUserId());
        String praisedName = praisedUser != null ? praisedUser.getRealName() : "学生" + praisedStudentId;
        Classes cls = classesMapper.selectById(classId);
        String className = cls != null ? cls.getClassName() : "班级" + classId;
        notificationService.notify(praised.getUserId(), "MORAL_PRAISE",
            "德育表扬", "你在「" + className + "」中因" + title + "获得 +" + creditValue + " 积分，继续保持！", post.getId());

        return Map.of("postId", post.getId(), "creditValue", creditValue);
    }

    @Override
    public List<Map<String, Object>> getMyReplies(Long userId) {
        Page<BbsReply> mpPage = new Page<>(1, 50);
        mpPage = bbsReplyMapper.selectPage(mpPage,
            new LambdaQueryWrapper<BbsReply>()
                .eq(BbsReply::getAuthorId, userId)
                .eq(BbsReply::getStatus, "normal")
                .orderByDesc(BbsReply::getCreateTime));
        List<BbsReply> replies = mpPage.getRecords();
        if (replies.isEmpty()) return Collections.emptyList();

        // 批量加载关联帖子
        Set<Long> postIds = replies.stream().map(BbsReply::getPostId).collect(Collectors.toSet());
        Map<Long, BbsPost> postMap = postIds.isEmpty() ? Collections.emptyMap() :
            bbsPostMapper.selectBatchIds(postIds).stream()
                .filter(p -> "normal".equals(p.getStatus()))
                .collect(Collectors.toMap(BbsPost::getId, p -> p));

        List<Map<String, Object>> result = new ArrayList<>();
        for (BbsReply r : replies) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("postId", r.getPostId());
            item.put("content", r.getContent());
            item.put("likeCount", r.getLikeCount());
            item.put("createTime", r.getCreateTime());
            BbsPost p = postMap.get(r.getPostId());
            item.put("postTitle", p != null ? p.getTitle() : "(帖子已删除)");
            item.put("postExists", p != null);
            result.add(item);
        }
        return result;
    }

    /** 查找德育表扬专用板块，优先匹配"德育风采"，找不到则取第一个可用板块 */
    private Long findMoralCategoryId() {
        BbsCategory moral = bbsCategoryMapper.selectOne(
            new LambdaQueryWrapper<BbsCategory>()
                .eq(BbsCategory::getStatus, 1)
                .like(BbsCategory::getName, "德育"));
        if (moral != null) return moral.getId();
        List<BbsCategory> all = bbsCategoryMapper.selectList(
            new LambdaQueryWrapper<BbsCategory>()
                .eq(BbsCategory::getStatus, 1)
                .orderByAsc(BbsCategory::getSortOrder));
        if (all.isEmpty()) throw new BusinessException(500, "论坛板块未初始化，请联系管理员");
        return all.get(0).getId();
    }

    // ==================== 热帖 & 活跃用户 ====================

    @Override
    public List<Map<String, Object>> getHotPosts(int limit, Long currentUserId) {
        LambdaQueryWrapper<BbsPost> w = new LambdaQueryWrapper<BbsPost>()
            .eq(BbsPost::getStatus, "normal")
            .orderByDesc(BbsPost::getReplyCount)
            .orderByDesc(BbsPost::getLikeCount);
        Page<BbsPost> mpPage = new Page<>(1, limit);
        mpPage = bbsPostMapper.selectPage(mpPage, w);
        return batchPostToMaps(mpPage.getRecords(), currentUserId);
    }

    @Override
    public List<Map<String, Object>> getActiveUsers(int limit) {
        // 近7天内有发帖或回复的用户，按活动次数排序
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        // 聚合发帖数
        List<Map<String, Object>> postCounts = bbsPostMapper.selectMaps(
            new LambdaQueryWrapper<BbsPost>()
                .select(BbsPost::getAuthorId)
                .eq(BbsPost::getStatus, "normal")
                .ge(BbsPost::getCreateTime, sevenDaysAgo));
        // 聚合回复数
        List<Map<String, Object>> replyCounts = bbsReplyMapper.selectMaps(
            new LambdaQueryWrapper<BbsReply>()
                .select(BbsReply::getAuthorId)
                .eq(BbsReply::getStatus, "normal")
                .ge(BbsReply::getCreateTime, sevenDaysAgo));

        Map<Long, Integer> activityMap = new HashMap<>();
        for (Map<String, Object> row : postCounts) {
            Long uid = (Long) row.get("authorId");
            if (uid != null) activityMap.merge(uid, 3, Integer::sum); // 发帖权重3
        }
        for (Map<String, Object> row : replyCounts) {
            Long uid = (Long) row.get("authorId");
            if (uid != null) activityMap.merge(uid, 1, Integer::sum); // 回复权重1
        }

        Set<Long> topIds = activityMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        if (topIds.isEmpty()) return Collections.emptyList();

        Map<Long, User> userMap = userMapper.selectBatchIds(topIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        return topIds.stream().map(uid -> {
            Map<String, Object> item = new HashMap<>();
            User u = userMap.get(uid);
            item.put("userId", uid);
            item.put("userName", u != null ? u.getRealName() : "未知");
            item.put("activityScore", activityMap.getOrDefault(uid, 0));
            return item;
        }).collect(Collectors.toList());
    }

}
