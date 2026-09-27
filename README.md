# minimal-cache

Đây là dự án học tập nhằm tự xây dựng một thư viện cache trong bộ nhớ ở mức tối giản. Mục tiêu hiện tại là hiểu cách lưu trữ dữ liệu, loại bỏ phần tử khi cache đầy và xử lý dữ liệu hết hạn.

## Chức năng hiện tại

- Lưu, đọc, xóa từng phần tử và xóa toàn bộ cache.
- Giới hạn số phần tử bằng `capacity`.
- Chọn chính sách loại bỏ phần tử: FIFO, LRU hoặc LFU.
- Có thể đặt thời gian sống (TTL) cho phần tử. Mặc định phần tử không hết hạn.

Tìm hiểu theo thứ tự: [cache trong bộ nhớ](docs/cache.md) → [eviction và các chính sách](docs/eviction.md) → [FIFO](docs/fifo.md), [LRU](docs/lru.md), [LFU](docs/lfu.md) → [TTL](docs/ttl.md). Nếu muốn dùng API ngay, xem [hướng dẫn sử dụng](docs/usage.md).

## Phạm vi giai đoạn này

Dự án đang tập trung vào các chức năng cơ bản. Cache **chưa hỗ trợ truy cập đồng thời** từ nhiều luồng và chưa có các chức năng nâng cao. Vì vậy, các phần như xử lý concurrency, mở rộng API và tối ưu hiệu năng sẽ được xem xét ở những giai đoạn sau.
