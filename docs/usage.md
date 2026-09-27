# Sử dụng minimal-cache

Đây là hướng dẫn dùng API hiện có của project trong một chương trình Java. Project đang ở giai đoạn học tập, chưa phát hành artifact để tải từ Maven Central và chưa hỗ trợ truy cập cache đồng thời từ nhiều luồng.

> **Lưu ý về Java modules:** Các ví dụ dưới đây áp dụng khi dùng mã nguồn hoặc artifact trên **classpath**. `module-info.java` hiện chưa export package `app.cache.builder` và các package chứa FIFO/LRU/LFU, nên một **named module** khác chưa thể dùng trực tiếp các lớp đó qua module path.

## Tạo cache và thao tác cơ bản

Khi tạo cache qua `CacheBuilder`, cần đặt `capacity` lớn hơn 0 và chọn một eviction policy. Nếu không đặt expiration policy, các phần tử mặc định không hết hạn theo thời gian.

```java
import app.cache.Cache;
import app.cache.builder.CacheBuilder;
import app.cache.eviction.fifo.FifoEvictionPolicy;

public class CacheUsageExample {
    public static void main(String[] args) {
        Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
            .capacity(2)
            .evictionPolicy(new FifoEvictionPolicy<>())
            .build();

        cache.put("A", "alpha");
        cache.put("B", "beta");
        cache.put("C", "gamma"); // FIFO evicts A.

        System.out.println(cache.get("A")); // Optional.empty
        System.out.println(cache.get("B")); // Optional[beta]
        System.out.println(cache.size());     // 2

        cache.remove("B"); // Returns an Optional containing the removed value.
        cache.clear();      // Removes all remaining entries.
    }
}
```

`get` và `remove` trả về `Optional`: rỗng nếu không tìm thấy key, có giá trị nếu tìm thấy. `put` thêm key mới hoặc cập nhật giá trị của key đang có. Không truyền `null` cho key hoặc value.

Nguồn trong repo: [Cache](../src/main/java/app/cache/Cache.java), [CacheBuilder](../src/main/java/app/cache/builder/CacheBuilder.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).

## Chọn chính sách loại bỏ

Thay `new FifoEvictionPolicy<>()` trong ví dụ trên bằng `new LruEvictionPolicy<>()` hoặc `new LfuEvictionPolicy<>()` để đổi cách chọn phần tử khi cache đầy. Các lớp tương ứng nằm trong `app.cache.eviction.lru` và `app.cache.eviction.lfu`.

- [FIFO](fifo.md): loại key được thêm vào sớm nhất.
- [LRU](lru.md): loại key lâu nhất chưa được sử dụng.
- [LFU](lfu.md): loại key có tần suất sử dụng thấp nhất.

Tạo một policy mới cho mỗi cache. Policy giữ trạng thái thứ tự hoặc tần suất riêng của cache đó; dùng chung một instance cho nhiều cache sẽ trộn trạng thái của chúng.

Nguồn trong repo: [FifoEvictionPolicy](../src/main/java/app/cache/eviction/fifo/FifoEvictionPolicy.java), [LruEvictionPolicy](../src/main/java/app/cache/eviction/lru/LruEvictionPolicy.java), [LfuEvictionPolicy](../src/main/java/app/cache/eviction/lfu/LfuEvictionPolicy.java).

## Đặt TTL

Để phần tử hết hạn sau một khoảng thời gian kể từ lần ghi gần nhất, thêm expiration policy vào builder:

```java
Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
    .capacity(2)
    .evictionPolicy(new FifoEvictionPolicy<>())
    .expirationPolicy(new TtlExpirationPolicy<>(Duration.ofMinutes(5)))
    .build();
```

Đoạn này dùng cùng các import như ví dụ đầu, cộng thêm `java.time.Duration` và `app.cache.expiration.ttl.TtlExpirationPolicy`. Mỗi lần `put` sẽ bắt đầu lại TTL của key; `get` không gia hạn TTL. Việc xóa entry hết hạn diễn ra khi cache kiểm tra nó, không có tác vụ dọn nền. Vì vậy `size()` có thể vẫn tính entry đã hết hạn nhưng chưa được dọn; `remove()` hiện không kiểm tra TTL trước khi trả giá trị.

TTL phải lớn hơn 0 và đủ nhỏ để biểu diễn bằng nanosecond. Xem [cách TTL hoạt động trong repo](ttl.md) để hiểu các thời điểm cache kiểm tra hạn dùng.

Nguồn trong repo: [TtlExpirationPolicy](../src/main/java/app/cache/expiration/ttl/TtlExpirationPolicy.java), [CacheBuilder](../src/main/java/app/cache/builder/CacheBuilder.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).
