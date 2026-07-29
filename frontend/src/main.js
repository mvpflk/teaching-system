/**
 * 主入口文件
 */
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import router from './router';
import App from './App.vue';

import 'element-plus/dist/index.css';
import './styles/index.scss';

// 按需注册实际使用的图标子集（~85个），替代全量 200+ 注册，节省 ~80KB
import {
  Aim,
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  ArrowUp,
  Bell,
  Calendar,
  Camera,
  ChatDotRound,
  ChatDotSquare,
  ChatLineRound,
  ChatLineSquare,
  Check,
  CircleCheck,
  CircleCheckFilled,
  CircleClose,
  Clock,
  Close,
  CloseBold,
  Coin,
  Collection,
  CollectionTag,
  Connection,
  Cpu,
  DataAnalysis,
  Delete,
  Document,
  DocumentAdd,
  DocumentCopy,
  Download,
  Edit,
  EditPen,
  Expand,
  Files,
  Finished,
  Fold,
  FolderOpened,
  Grid,
  HomeFilled,
  House,
  InfoFilled,
  Key,
  Lightning,
  Link,
  List,
  Loading,
  Lock,
  MagicStick,
  Medal,
  Message,
  Microphone,
  Monitor,
  Moon,
  MoreFilled,
  Notebook,
  Notification,
  Operation,
  Opportunity,
  Orange,
  Paperclip,
  PictureFilled,
  Plus,
  Printer,
  Promotion,
  Rank,
  Reading,
  Refresh,
  School,
  Search,
  Setting,
  Star,
  StarFilled,
  SuccessFilled,
  Sunny,
  SwitchButton,
  Tickets,
  Timer,
  Tools,
  Top,
  TrendCharts,
  Trophy,
  TrophyBase,
  Upload,
  UploadFilled,
  User,
  UserFilled,
  View,
  Warning,
  WarningFilled,
} from '@element-plus/icons-vue';

const app = createApp(App);
const pinia = createPinia();
app.use(pinia);
app.use(router);

const icons = {
  Aim,
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  ArrowUp,
  Bell,
  Calendar,
  Camera,
  ChatDotRound,
  ChatDotSquare,
  ChatLineRound,
  ChatLineSquare,
  Check,
  CircleCheck,
  CircleCheckFilled,
  CircleClose,
  Clock,
  Close,
  CloseBold,
  Coin,
  Collection,
  CollectionTag,
  Connection,
  Cpu,
  DataAnalysis,
  Delete,
  Document,
  DocumentAdd,
  DocumentCopy,
  Download,
  Edit,
  EditPen,
  Expand,
  Files,
  Finished,
  Fold,
  FolderOpened,
  Grid,
  HomeFilled,
  House,
  InfoFilled,
  Key,
  Lightning,
  Link,
  List,
  Loading,
  Lock,
  MagicStick,
  Medal,
  Message,
  Microphone,
  Monitor,
  Moon,
  MoreFilled,
  Notebook,
  Notification,
  Operation,
  Opportunity,
  Orange,
  Paperclip,
  PictureFilled,
  Plus,
  Printer,
  Promotion,
  Rank,
  Reading,
  Refresh,
  School,
  Search,
  Setting,
  Star,
  StarFilled,
  SuccessFilled,
  Sunny,
  SwitchButton,
  Tickets,
  Timer,
  Tools,
  Top,
  TrendCharts,
  Trophy,
  TrophyBase,
  Upload,
  UploadFilled,
  User,
  UserFilled,
  View,
  Warning,
  WarningFilled,
};
Object.entries(icons).forEach(([key, comp]) => app.component(key, comp));

app.mount('#app');

// 移动端弹窗滚动穿透防止
// 监听 el-dialog 的 overlay 出现/消失，给 body 加 dialog-open 类
const observer = new MutationObserver(() => {
  const hasOpenDialog =
    document.querySelector('.el-overlay')?.style?.display !== 'none' &&
    document.querySelector('.el-overlay[style*="display: none"]') === null &&
    document.querySelector('.el-overlay') !== null;
  document.body.classList.toggle('dialog-open', hasOpenDialog);
});
observer.observe(document.body, {
  childList: true,
  subtree: true,
  attributes: true,
  attributeFilter: ['style', 'class'],
});

// 移动端网络状态监听
window.addEventListener('offline', () => {
  const tip = document.createElement('div');
  tip.id = 'net-tip';
  tip.style.cssText = 'position:fixed;top:0;left:0;right:0;z-index:9999;background:var(--el-color-danger);color:#fff;text-align:center;padding:10px 16px;font-size:14px;';
  tip.textContent = '网络已断开，部分功能不可用';
  document.body.prepend(tip);
});
window.addEventListener('online', () => {
  document.getElementById('net-tip')?.remove();
  window.dispatchEvent(new CustomEvent('network:online'));
});
