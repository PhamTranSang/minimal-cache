# Chính sách FIFO trong minimal-cache

Tài liệu này đi sâu vào FIFO. Đọc [cache trong bộ nhớ](cache.md) và [tổng quan eviction](eviction.md) trước để nắm `capacity` và lý do cache cần chọn một key để loại.

## FIFO chọn phần tử nào?

FIFO (*First In, First Out*) loại key được **đưa vào cache sớm nhất** trong số các key còn lại. `FifoEvictionPolicy` giữ thứ tự đó bằng một hàng đợi (`ArrayDeque`): đầu hàng đợi là key sẽ bị loại tiếp theo.

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

- `onPut(key)` chỉ đưa key mới vào cuối hàng đợi; `contains()` ngăn một key xuất hiện hai lần.
- `onGet(key)` dùng hành vi mặc định của `EvictionPolicy`, nên không đổi hàng đợi.
- `onRemove(key)` xóa key khỏi hàng đợi khi cache xóa thủ công hoặc do hết hạn.
- `evict()` lấy và bỏ key ở đầu hàng đợi; `InMemoryCache` dùng key đó để xóa giá trị tương ứng khỏi `HashMap`.
- `clear()` xóa toàn bộ thứ tự FIFO cùng lúc cache xóa dữ liệu.

`contains()` trong `onPut` và `remove(key)` phải tìm trong hàng đợi, nên chi phí tăng theo số phần tử. Đây là đặc điểm của cách cài đặt hiện tại, không phải quy tắc bắt buộc của FIFO.

Nguồn trong repo: [EvictionPolicy](../src/main/java/app/cache/eviction/EvictionPolicy.java), [FifoEvictionPolicy](../src/main/java/app/cache/eviction/fifo/FifoEvictionPolicy.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).

Đọc tiếp: [Chính sách LRU](lru.md) để xem thứ tự thay đổi khi một key được đọc hoặc cập nhật.
