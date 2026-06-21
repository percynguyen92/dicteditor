[English](README_EN.md)

# DictEditor

DictEditor là một ứng dụng chỉnh sửa từ điển Android. Ứng dụng được thiết kế để quản lý các từ điển **Trung-Việt**.

## Tính năng

- **Quản lý nghĩa của từ**: Hỗ trợ chỉnh sửa trực tiếp, thêm mới, xóa và kéo thả để sắp xếp lại thứ tự của các nghĩa của từ.
- **Tìm và thay thế**: hỗ trợ tìm và thay thế hàng loạt với Regex.
- **Nhập & Xuất dữ liệu**: Nhập dữ liệu và xuất ra file .txt với định dạng như sau:
```
	愉悦度=độ vui vẻ
	愁衣食=lo cơm áo
	惹火了=chọc giận
	惹恼了=chọc giận
	惹怒了=chọc giận
	惹姐姐=Nhã tỷ tỷ
	惹哭了=chọc khóc
	惹到了=chọc phải
```
- **Hoàn tác & Làm lại (Undo & Redo)**: Hỗ trợ đầy đủ Undo/Redo.

## Công nghệ sử dụng

- **Framework**: Android SDK (Kotlin), Jetpack Compose
- **Design System**: Material Design 3 + Glassmorphism effect by Haze package
- **Kiến trúc**: MVVM (Model-View-ViewModel) với StateFlow

## Bắt đầu sử dụng

### Yêu cầu hệ thống

- Android Studio Koala hoặc mới hơn
- Android SDK 34+
- Java JDK 17
- Thiết bị Android: Android 7.0 (API level 24) trở lên


### Hướng dẫn Build và Chạy thử

1. Clone repository này về máy.
2. Mở thư mục dự án bằng Android Studio.
3. Chờ Gradle đồng bộ và tải các thư viện phụ thuộc.
4. Chạy ứng dụng trên Emulator Android hoặc thiết bị vật lý (khuyến nghị API level 26 trở lên)

###Todo:
- **Tích hợp AI**:Dùng AI để gợi ý nghĩa, hiện tại chưa hỗ Kết nối với các dịch vụ AI thông qua 1 app portal khác, tôi sẽ cân nhắc tích hợp vào app này hoặc public portal.
- **SQLite**: Hiện tại app đang đọc ghi trực tiếp trên file txt. Đang có ý định chuyển sang dùng SQLite để tăng tốc xử lý các file VP và NE lớn.