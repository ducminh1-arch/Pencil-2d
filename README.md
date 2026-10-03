# Pencil 2D: Draw & Animations

Ứng dụng vẽ tay kết hợp làm hoạt hình 2D theo dạng từng khung hình (frame-by-frame / flipbook animation), được xây dựng chuẩn theo phong cách và tính năng của ứng dụng [Pencil 2d - Draw & Animations trên Apple App Store](https://apps.apple.com/us/app/pencil-2d-draw-animations/id6578446729?l=vi).

---

## 📁 Cấu trúc dự án

```
d:\Pencil2d\
├── android/            # Dự án Android Native (Kotlin + Jetpack Compose)
│   ├── app/            # Mã nguồn ứng dụng, UI, Canvas Engine, Exporters
│   │   └── build/outputs/apk/debug/app-debug.apk # File APK đã build sẵn sàng cài đặt
│   ├── gradle/
│   ├── build.gradle.kts
│   └── gradlew.bat
└── ios/                # Dự án iOS Native (Swift + SwiftUI + PencilKit)
    ├── Pencil2D.xcodeproj # File project mở trực tiếp trong Xcode
    └── Pencil2D/
        ├── App/        # SwiftUI App Entry
        ├── Models/     # Cấu trúc dữ liệu Project > Layers > Frames
        ├── DrawingEngine/ # PencilKit, FloodFill, FrameCompositor
        ├── Timeline/   # Scrubber timeline bar, navigation
        ├── ExportEngine/# AVFoundation (MP4), ImageIO (GIF)
        ├── Storage/    # Lưu trữ Project JSON & layer PNG
        ├── ViewModels/ # MVVM ViewModels
        └── Views/      # Giao diện khớp chính xác ảnh App Store
```

---

## 🎨 1. Tính năng cốt lõi (Core Modules)

### ✏️ Drawing Engine (Bộ vẽ)
- **Công cụ vẽ đầy đủ**:
  - **Pen (Bút mực)**: Nét vẽ mịn, chống răng cưa (anti-aliased).
  - **Pencil (Bút chì)**: Nét vẽ mềm phong cách sketch.
  - **Eraser (Tẩy)**: Xóa theo nét vẽ trên active layer.
  - **Fill (Paint Bucket)**: Thuật toán Flood Fill BFS tốc độ cao tô màu vùng khép kín.
  - **Color Palette**: Bảng màu studio phong phú (Đen, Trắng, Vàng chủ đạo, Đỏ, Cam, Xanh lá, Xanh dương, Tím, Nâu).
  - **Thanh trượt kích thước cọ (Brush Width)**: Xem trước độ dày nét vẽ trực quan.
- **Hỗ trợ cảm ứng & áp lực bút**:
  - Hỗ trợ Apple Pencil trên iOS với PencilKit.
  - Hỗ trợ bút cảm ứng (stylus) và chạm đa điểm trên Android.
- **Làm mịn nét vẽ**: Thuật toán nội suy Quadratic Bézier curves giúp đường vẽ mượt mà, không bị gấp khúc.

### 🧅 Onion Skinning (Vỏ hành - 2D Animation Must-Have)
- Hiển thị mờ khung hình trước (**phủ màu đỏ**) và khung hình sau (**phủ màu xanh lá**) giúp người dùng canh chuyển động chính xác.
- Nút bật/tắt nhanh (`🧅`) ngay trên thanh công cụ trên cùng.
- Tự động ẩn Onion Skin khi phát animation để xem phim mượt mà.

### 📚 Hệ thống nhiều lớp vẽ (Multi-Layer)
- Cấu trúc: Project > Layers (Background, Character, Foreground) > Frames.
- Bật/tắt hiển thị (visibility eye), khóa lớp vẽ (lock), tùy chỉnh độ trong suốt (opacity).
- Thêm layer mới, chọn layer đang vẽ.

### 📐 Nhiều kích thước khung vẽ (Multiple Canvas Sizes)
Khớp màn hình chọn tỷ lệ trong ảnh mẫu:
- **1:1** (Square - 1080x1080): Phù hợp Instagram/mạng xã hội.
- **16:9** (Landscape - 1920x1080): Chuẩn video YouTube/màn hình rộng.
- **4:3** (Classic - 1440x1080): Tỷ lệ máy tính bảng / TV cổ điển.
- **9:16** (Portrait - 1080x1920): Chuẩn video ngắn TikTok, Reels, Shorts.

### ⏱️ Timeline & Quản lý khung hình (Frame Management)
- Thanh timeline cuộn ngang hiển thị thumbnail từng frame.
- **Thao tác**: Thêm frame trống (`+`), nhân bản frame (`duplicate`), xóa frame (`delete`).
- Tùy chỉnh tốc độ khung hình (FPS: 6, 12, 16, 24, 30 FPS).
- Nút Play/Pause chạy hoạt hình trực tiếp trên màn hình vẽ.

### 🎬 Bộ xuất phim (Export Engine)
- **MP4 Video**:
  - iOS: Sử dụng `AVFoundation` (`AVAssetWriter` + `AVAssetWriterInputPixelBufferAdaptor`) xuất video H.264 MP4.
  - Android: Sử dụng `MediaCodec` (H.264 AVC) + `MediaMuxer` kết xuất video MP4 trực tiếp.
- **Animated GIF**:
  - iOS: `ImageIO` (`CGImageDestinationCreateWithData`).
  - Android: Bộ mã hóa GIF thuần Kotlin với nén LZW.
- **PNG Sequence**: Xuất chuỗi ảnh PNG chất lượng gốc.
- Tích hợp Share Sheet hệ thống để lưu vào Photo Library hoặc chia sẻ qua Zalo, Messenger, AirDrop, v.v.

### 📁 Bộ nhớ & Dự án mẫu (Sample Projects)
- Dự án lưu tự động dạng JSON và ảnh PNG.
- Sẵn 2 dự án mẫu có thể chơi ngay khi vừa mở ứng dụng:
  1. **Project 1 (Stickman)**: Hoạt hình người que sút bóng đá (như ảnh App Store).
  2. **Project 2 (Bouncing Ball)**: Hoạt hình quả bóng nảy kinh điển.

---

## 🚀 Hướng dẫn chạy

### 📱 1. Android (Kotlin + Jetpack Compose)
- **Đã build sẵn APK**:
  File cài đặt: `d:\Pencil2d\android\app\build\outputs\apk\debug\app-debug.apk`
- **Build bằng dòng lệnh**:
  ```bash
  cd d:\Pencil2d\android
  .\gradlew.bat assembleDebug
  ```
- **GitLab Repository**:
  - Remote: `https://gitlab.com/sondeptrai/pencil2d_android.git`
  - Đã đẩy toàn bộ source code lên branch: `feature/pencil2d-app`

### 🍎 2. iOS (Swift + SwiftUI + PencilKit)
- **Mở dự án trong Xcode**:
  ```bash
  cd d:\Pencil2d\ios
  open Pencil2D.xcodeproj
  ```
- **Yêu cầu môi trường**:
  - macOS với Xcode 15+
  - iOS Deployment Target: iOS 16.0+
- **GitLab Repository**:
  - Remote: `https://gitlab.com/sondeptrai/pencil2d_ios.git`
  - Mã nguồn đã được commit và sẵn sàng tại `d:\Pencil2d\ios`.
