# Chính sách FIFO trong minimal-cache

Tài liệu này đi sâu vào FIFO. Đọc [cache trong bộ nhớ](cache.md) và [tổng quan eviction](eviction.md) trước để nắm `capacity` và lý do cache cần chọn một key để loại.

## FIFO chọn phần tử nào?

FIFO (*First In, First Out*) loại key được **đưa vào cache sớm nhất** trong số các key còn lại. `FifoEvictionPolicy` giữ thứ tự đó bằng một `EntryList` (xem [tổng quan eviction](eviction.md)) dùng như hàng đợi: đầu hàng đợi là key sẽ bị loại tiếp theo.

Giả sử `capacity = 3`, không cấu hình TTL và ban đầu cache rỗng. Trong bảng, `put(A)` là cách viết gọn cho `put(A, giá trị)`:

| Thao tác | Hàng đợi sau thao tác, từ đầu đến cuối | Điều xảy ra |
| --- | --- | --- |
| `put(A)`, `put(B)`, `put(C)` | `A → B → C` | Cache vừa đầy. |
| `get(A)` | `A → B → C` | Đọc A không đổi thứ tự FIFO. |
| `put(D)` | `B → C → D` | A bị loại để nhường chỗ cho D. |
| `put(B)` | `B → C → D` | Cập nhật B không làm B thành phần tử mới. |

Điểm dễ nhầm là FIFO dựa vào **lần thêm key vào cache**, không dựa vào lần đọc hoặc cập nhật gần nhất. Nếu một key bị xóa rồi được thêm lại, lần thêm mới đưa nó xuống cuối hàng đợi.

Nguồn trong repo: [FifoEvictionPolicy](../src/main/java/app/cache/eviction/fifo/FifoEvictionPolicy.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).

## Code phối hợp với FIFO ra sao?

- `onAdd(entry)` đưa entry mới vào cuối hàng đợi.
- `onAccess(entry)` dùng hành vi mặc định của `EvictionPolicy` (không làm gì), nên đọc hoặc cập nhật key không đổi hàng đợi.
- `onRemove(entry)` tháo entry khỏi hàng đợi khi cache xóa thủ công hoặc do hết hạn.
- `evict()` lấy và tháo entry ở đầu hàng đợi; `InMemoryCache` dùng key của entry đó để xóa nó khỏi `HashMap`.
- `clear()` xóa toàn bộ thứ tự FIFO cùng lúc cache xóa dữ liệu.

Policy nhận thẳng entry nên không phải tìm key trong hàng đợi: mọi thao tác chỉ đổi vài liên kết. Bản cài đặt đầu tiên dùng `ArrayDeque` phải duyệt hàng đợi khi kiểm tra và xóa key, nên chậm dần theo số phần tử.

Nguồn trong repo: [EvictionPolicy](../src/main/java/app/cache/eviction/EvictionPolicy.java), [FifoEvictionPolicy](../src/main/java/app/cache/eviction/fifo/FifoEvictionPolicy.java), [EntryList](../src/main/java/app/cache/entry/EntryList.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).

Đọc tiếp: [Chính sách LRU](lru.md) để xem thứ tự thay đổi khi một key được đọc hoặc cập nhật.
