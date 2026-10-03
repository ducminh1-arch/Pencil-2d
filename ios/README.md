# Pencil 2D: Draw & Animations (iOS Native - Swift)

Ứng dụng vẽ tay và làm hoạt hình 2D theo từng khung hình (frame-by-frame / flipbook) trên iOS (iPhone & iPad).
Được thiết kế dựa trên phong cách ứng dụng Pencil 2D: Draw Animations trên Apple App Store.

## Tính năng chính
1. **Drawing Engine & Apple Pencil Support**:
   - Tích hợp Apple Pencil độ trễ cực thấp với PencilKit và CoreGraphics.
   - Các công cụ: Pen, Pencil, Eraser, Fill (Flood Fill / Paint Bucket).
   - Tùy chỉnh màu sắc bảng màu studio và kích thước nét vẽ (brush width).
2. **Timeline & Frame Management**:
   - Quản lý cây dữ liệu Project > Layers > Frames.
   - Thêm frame trống (+), nhân bản (duplicate), xóa frame, xem thumbnail preview.
   - Tùy chỉnh tốc độ khung hình (FPS: 6, 12, 16, 24, 30 FPS).
3. **Onion Skinning (Vỏ hành)**:
   - Hiển thị mờ khung hình trước (phủ màu đỏ) và khung hình sau (phủ màu xanh lá) để canh chuyển động.
   - Bật/tắt nhanh bằng nút toggle trên thanh điều khiển.
4. **Hệ thống nhiều Layer (Lớp vẽ)**:
   - Hỗ trợ nhiều layer: Background, Character, Foreground.
   - Bật/tắt hiển thị (visibility eye), khóa layer (lock), thanh trượt độ trong suốt (opacity).
5. **Multiple Canvas Sizes (Kích thước khung vẽ)**:
   - Hỗ trợ đầy đủ các tỷ lệ: 1:1 (Square), 16:9 (Landscape), 4:3 (Classic Tablet), 9:16 (Portrait Shorts/Reels).
6. **Export Engine**:
   - Xuất video MP4 độ phân giải cao bằng AVFoundation (`AVAssetWriter`).
   - Xuất Animated GIF bằng ImageIO (`CGImageDestination`).
   - Xuất chuỗi ảnh PNG sequence và chia sẻ qua UIActivityViewController.

## Cách mở dự án
1. Mở thư mục bằng Xcode: `open Pencil2D.xcodeproj`
2. Chọn thiết bị đích (iPhone Simulator hoặc iPad)
3. Nhấn **Run** (Cmd + R) để khởi chạy.
