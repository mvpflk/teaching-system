/** 响应式断点常量
 *  SMALL_MOBILE: < 480px（极小屏，如 iPhone SE）
 *  MOBILE:      < 768px（标准手机，注意 mobile.css 实际使用 767px 避免 iPad mini 横屏误触）
 *  TABLET:      768-1023px（平板竖屏，含 iPad mini 横屏）
 *  Desktop:     >= 1024px
 *
 *  注意：mobile.css 中用 767px 而非 768px，因为 iPad mini 分辨率为 768px 横屏时不应触发手机断点。
 *  项目约 200+ 处硬编码 @media (max-width: 768px)，建议逐步替换为引用此常量。
 */
export const SMALL_MOBILE = 480
export const MOBILE = 768
export const TABLET = 1024
export const WIDE = 1920
export const ULTRAWIDE = 2560
