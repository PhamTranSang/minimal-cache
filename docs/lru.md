# Chính sách LRU trong minimal-cache

Tài liệu này đi sâu vào LRU, tiếp nối [tổng quan eviction](eviction.md). Nếu chưa quen với `capacity` và các thao tác cache, đọc [cache trong bộ nhớ](cache.md) trước.

## LRU chọn phần tử nào?

LRU (*Least Recently Used*) loại key **lâu nhất chưa được sử dụng**. Trong implementation hiện tại, đọc một key còn hiệu lực bằng `get` hoặc thêm/cập nhật bằng `put` đều tính là sử dụng key đó. Key được sử dụng gần đây nhất nằm cuối thứ tự; key lâu nhất nằm đầu và sẽ bị loại tiếp theo.

Giả sử `capacity = 3`, không cấu hình TTL và ban đầu cache rỗng. `put(A)` trong bảng là cách viết gọn cho `put(A, giá trị)`:

| Thao tác | Thứ tự LRU, từ lâu nhất đến gần nhất | Điều xảy ra |
| --- | --- | --- |
| `put(A)`, `put(B)`, `put(C)` | `A → B → C` | Cache vừa đầy. |
| `get(A)` | `B → C → A` | A được chuyển xuống cuối vì vừa được đọc. |
| `put(D)` | `C → A → D` | B bị loại để nhường chỗ cho D. |
| `put(A)` | `C → D → A` | Cập nhật A làm A trở thành key dùng gần nhất. |

Với cùng ba thao tác đầu, FIFO sẽ loại **A** khi thêm D vì A được đưa vào sớm nhất; LRU loại **B** vì A vừa được đọc. Đây là khác biệt cốt lõi giữa hai chính sách.

Nguồn trong repo: [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java), [LruEvictionPolicy](../src/main/java/app/cache/eviction/lru/LruEvictionPolicy.java).

## Repo giữ thứ tự sử dụng bằng gì?

`LruAccessOrder` kết hợp hai cấu trúc:

- `HashMap<K, LruNode<K>>` tìm nhanh node tương ứng với một key.
- Danh sách liên kết đôi nối các node bằng `prev` và `next`. `head` là key lâu nhất chưa được dùng; `tail` là key vừa được dùng gần nhất.

Khi một key được sử dụng lại, `moveToTail` tháo node khỏi vị trí cũ rồi gắn vào cuối danh sách. Khi cần eviction, `removeLeastRecentlyUsed` tháo `head` và xóa key tương ứng khỏi map của policy. `InMemoryCache` sau đó xóa key đó khỏi map chứa giá trị. Map trong policy và map chứa giá trị có vai trò khác nhau: một bên tìm node để cập nhật thứ tự, một bên lưu dữ liệu cache.

Nguồn trong repo: [LruAccessOrder](../src/main/java/app/cache/eviction/lru/LruAccessOrder.java), [LruNode](../src/main/java/app/cache/eviction/lru/LruNode.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).

## Các thao tác tác động đến LRU ra sao?

- `onPut(key)` thêm node mới vào cuối, hoặc chuyển node đã có xuống cuối.
- `onGet(key)` chuyển node xuống cuối nếu key có trong policy. `InMemoryCache` chỉ gọi thao tác này sau khi xác nhận giá trị còn hiệu lực.
- `onRemove(key)` xóa node khi key bị xóa thủ công hoặc hết hạn.
- `evict()` lấy key ở đầu danh sách; `clear()` xóa toàn bộ map và thứ tự liên kết.

Điểm dễ nhầm: một lần `get` không tìm thấy key không làm đổi thứ tự LRU. Tương tự, key đã hết hạn bị xóa khỏi cache và policy thay vì được chuyển xuống cuối. Việc chuyển node đã tìm thấy và loại node đầu danh sách chỉ cần thay đổi một số liên kết; tra key trong `HashMap` có chi phí trung bình hằng số. Đây là đặc điểm của cách cài đặt hiện tại, chưa xét truy cập đồng thời.

Nguồn trong repo: [LruEvictionPolicy](../src/main/java/app/cache/eviction/lru/LruEvictionPolicy.java), [LruAccessOrder](../src/main/java/app/cache/eviction/lru/LruAccessOrder.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).

Đọc tiếp: [Chính sách LFU](lfu.md) để xem cách chọn key theo tần suất sử dụng.
