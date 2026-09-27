# Chính sách LFU trong minimal-cache

Tài liệu này đi sâu vào LFU, tiếp nối [tổng quan eviction](eviction.md). Cache vẫn giới hạn số phần tử bằng `capacity`; LFU thay đổi cách chọn key bị loại khi cache đầy.

## LFU chọn phần tử nào?

LFU (*Least Frequently Used*) loại key có **tần suất sử dụng thấp nhất**. Trong repo này, key mới bắt đầu ở tần suất 1. Mỗi lần `get` một giá trị còn hiệu lực hoặc `put` cập nhật một key đang có sẽ tăng tần suất của key đó thêm 1. Nếu nhiều key cùng tần suất thấp nhất, key vào nhóm tần suất đó sớm nhất bị loại trước.

Giả sử `capacity = 3`, không cấu hình TTL và cache ban đầu rỗng. `put(A)` là cách viết gọn cho `put(A, giá trị)`:

| Thao tác | Nhóm tần suất sau thao tác, theo thứ tự cũ → mới | Điều xảy ra |
| --- | --- | --- |
| `put(A)`, `put(B)`, `put(C)` | `1: A → B → C` | Cả ba key có tần suất 1. |
| `get(A)`, `get(B)` | `1: C`; `2: A → B` | A và B được đọc, mỗi key tăng lên 2. |
| `put(D)` | `1: D`; `2: A → B` | C có tần suất thấp nhất nên bị loại. |
| `get(D)` | `2: A → B → D` | D chuyển sang nhóm tần suất 2. |
| `put(E)` | `1: E`; `2: B → D` | Cả A, B, D cùng tần suất 2; A vào nhóm này sớm nhất nên bị loại. |

Nguồn trong repo: [LfuEvictionPolicy](../src/main/java/app/cache/eviction/lfu/LfuEvictionPolicy.java), [LfuFrequencyStructure](../src/main/java/app/cache/eviction/lfu/LfuFrequencyStructure.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).

## Repo giữ tần suất và thứ tự ra sao?

`LfuFrequencyStructure` có map `nodes` để tìm node theo key, map `buckets` để tìm nhóm theo tần suất, và `minFreq` để biết nhóm nào bị xét trước. Mỗi `LfuNode` giữ key, tần suất và hai liên kết `prev`/`next`. Các node cùng tần suất nằm trong một `FrequencyBucket`: đầu nhóm là node vào nhóm sớm nhất, cuối nhóm là node mới vào nhóm gần đây nhất.

Khi key được sử dụng, code tháo node khỏi bucket cũ, tăng tần suất, rồi gắn node vào cuối bucket mới. Khi eviction, code lấy node đầu tiên của bucket `minFreq`. Vì vậy, quy tắc hòa là **thứ tự vào nhóm tần suất hiện tại**, không phải thứ tự được thêm lần đầu vào cache.

Nguồn trong repo: [LfuFrequencyStructure](../src/main/java/app/cache/eviction/lfu/LfuFrequencyStructure.java), [FrequencyBucket](../src/main/java/app/cache/eviction/lfu/FrequencyBucket.java), [LfuNode](../src/main/java/app/cache/eviction/lfu/LfuNode.java).

## Các thao tác tác động đến LFU ra sao?

- `onPut(key)` thêm key mới với tần suất 1; nếu key đã có, nó tăng tần suất như một lần sử dụng.
- `onGet(key)` tăng tần suất khi cache trả về một giá trị còn hiệu lực. Đọc key không tồn tại hoặc đã hết hạn không làm tăng tần suất.
- `onRemove(key)` xóa node khỏi nhóm tần suất khi key bị xóa thủ công hoặc do hết hạn.
- `evict()` lấy key ở đầu bucket có `minFreq`; `clear()` xóa toàn bộ node, bucket và đặt lại `minFreq`.

Đây là tần suất tích lũy trong thời gian key còn ở cache, chưa có cơ chế giảm tần suất theo thời gian. `findMinFrequency()` quét các bucket hiện có khi cần tìm lại mức thấp nhất, nên không nên mặc định rằng mọi thao tác của implementation này đều có chi phí hằng số. Repo cũng chưa hỗ trợ truy cập đồng thời.

Nguồn trong repo: [LfuEvictionPolicy](../src/main/java/app/cache/eviction/lfu/LfuEvictionPolicy.java), [LfuFrequencyStructure](../src/main/java/app/cache/eviction/lfu/LfuFrequencyStructure.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).

Đọc tiếp: [TTL và cách cache xử lý phần tử hết hạn](ttl.md).
