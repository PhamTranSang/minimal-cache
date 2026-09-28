# Sử dụng minimal-cache

Đây là hướng dẫn dùng API hiện có của project trong một chương trình Java. Project đang ở giai đoạn học tập, chưa phát hành artifact để tải từ Maven Central và chưa hỗ trợ truy cập cache đồng thời từ nhiều luồng.

Cache được tạo qua DSL `Caches.create(...)`. Module chỉ export hai package: `app.cache` (chứa `Cache`, `Caches`, `Ticker`) và `app.cache.exception`. Các package còn lại như eviction policy, expiration policy và cấu hình nội bộ là chi tiết cài đặt; từ một named module khác, compiler sẽ báo `package ... is not visible` nếu import chúng.

## Tạo cache và thao tác cơ bản

Truyền vào `Caches.create` một lambda cấu hình, rồi gọi `build()`. Lambda phải chọn đúng một eviction policy; chọn lần hai sẽ báo lỗi cấu hình. Không gọi `capacity` thì mặc định là 100; nếu gọi, giá trị phải lớn hơn 0. Không gọi `ttl` thì các phần tử không hết hạn theo thời gian.

```java
import app.cache.Cache;
import app.cache.Caches;

public class CacheUsageExample {
    public static void main(String[] args) {
        Cache<String, String> cache = Caches.create(config -> config
            .capacity(2)
            .fifo()
        ).build();

        cache.put("A", "alpha");
        cache.put("B", "beta");
        cache.put("C", "gamma"); // FIFO evicts A.

        System.out.println(cache.get("A")); // Optional.empty
        System.out.println(cache.get("B")); // Optional[beta]
        System.out.println(cache.size());     // 2

        cache.remove("B"); // Returns an Optional containing the removed value.
        cache.clear();      // Removes all remaining entries.

        Cache<String, String> defaultCache = Caches.create(config -> config.lru()).build();
        // defaultCache has capacity 100 and no expiration.
    }
}
```

`get` và `remove` trả về `Optional`: rỗng nếu không tìm thấy key, có giá trị nếu tìm thấy. `put` thêm key mới hoặc cập nhật giá trị của key đang có. Không truyền `null` cho key hoặc value.

Nguồn trong repo: [Cache](../src/main/java/app/cache/Cache.java), [Caches](../src/main/java/app/cache/Caches.java), [CacheConfig](../src/main/java/app/cache/config/CacheConfig.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).

## Chọn chính sách loại bỏ

Thay `.fifo()` trong ví dụ trên bằng `.lru()` hoặc `.lfu()` để đổi cách chọn phần tử khi cache đầy.

- [FIFO](fifo.md): loại key được thêm vào sớm nhất.
- [LRU](lru.md): loại key lâu nhất chưa được sử dụng.
- [LFU](lfu.md): loại key có tần suất sử dụng thấp nhất.

Mỗi lần `build()` tạo một eviction policy mới, nên hai cache tạo từ cùng một cấu hình vẫn giữ thứ tự hoặc tần suất riêng, không trộn trạng thái của nhau.

Nguồn trong repo: [Caches](../src/main/java/app/cache/Caches.java), [FifoEvictionPolicy](../src/main/java/app/cache/eviction/fifo/FifoEvictionPolicy.java), [LruEvictionPolicy](../src/main/java/app/cache/eviction/lru/LruEvictionPolicy.java), [LfuEvictionPolicy](../src/main/java/app/cache/eviction/lfu/LfuEvictionPolicy.java).

## Đặt TTL

Để phần tử hết hạn sau một khoảng thời gian kể từ lần ghi gần nhất, gọi `ttl`:

```java
Cache<String, String> cache = Caches.create(config -> config
    .capacity(2)
    .fifo()
    .ttl(Duration.ofMinutes(5))
).build();
```

Đoạn này dùng cùng các import như ví dụ đầu, cộng thêm `java.time.Duration`. Mỗi lần `put` sẽ bắt đầu lại TTL của key; `get` không gia hạn TTL. Việc xóa entry hết hạn diễn ra khi cache kiểm tra nó, không có tác vụ dọn nền. Vì vậy `size()` có thể vẫn tính entry đã hết hạn nhưng chưa được dọn; `remove()` hiện không kiểm tra TTL trước khi trả giá trị.

TTL phải lớn hơn 0 và đủ nhỏ để biểu diễn bằng nanosecond. Giá trị không hợp lệ bị báo lỗi khi gọi `build()`. Xem [cách TTL hoạt động trong repo](ttl.md) để hiểu các thời điểm cache kiểm tra hạn dùng.

### Thay nguồn thời gian bằng `ticker`

Cache đo thời gian trôi qua bằng một `Ticker`, mặc định là `Ticker.systemTicker()` (đọc `System.nanoTime()`). Gọi `ticker(...)` để thay nguồn thời gian này, thường là trong test, để đẩy thời gian lên theo ý mà không phải chờ thật:

```java
long[] now = {0};
Cache<String, String> cache = Caches.create(config -> config
    .lru()
    .ttl(Duration.ofMinutes(5))
    .ticker(() -> now[0])
).build();

cache.put("A", "alpha");
now[0] += Duration.ofMinutes(5).toNanos();
System.out.println(cache.get("A")); // Optional.empty
```

`Ticker` chỉ dùng để đo khoảng thời gian, không phải giờ đồng hồ. Không gọi `ttl` thì cache không đọc ticker. Truyền `null` cho `ticker` khi đã gọi `ttl` sẽ báo lỗi cấu hình khi `build()`.

Nguồn trong repo: [Caches](../src/main/java/app/cache/Caches.java), [Ticker](../src/main/java/app/cache/Ticker.java), [TtlExpirationPolicy](../src/main/java/app/cache/expiration/ttl/TtlExpirationPolicy.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).

## Xử lý lỗi

Mọi lỗi do cách cấu hình hoặc cách gọi cache đều kế thừa `app.cache.exception.CacheException`, một `RuntimeException`. Bắt `CacheException` là đủ để xử lý chung; bắt lớp con khi cần phân biệt:

| Exception | Khi nào |
| --- | --- |
| `IncompleteCacheConfigurationException` | Lambda cấu hình không chọn eviction policy. |
| `InvalidCacheConfigurationException` | Truyền `null` cho `Caches.create`, lambda không trả về đúng object cấu hình, chọn eviction policy lần hai, hoặc `ticker` là `null` khi đã gọi `ttl`. |
| `InvalidCapacityException` | `capacity` nhỏ hơn hoặc bằng 0. |
| `InvalidTtlException` | TTL là `null`, nhỏ hơn hoặc bằng 0, hoặc quá lớn để biểu diễn bằng nanosecond. |
| `InvalidCacheEntryException` | Key hoặc value là `null` khi gọi `get`, `put`, `remove`. |

Nếu cache ném `IllegalStateException`, đó là lỗi bên trong thư viện (trạng thái nội bộ không nhất quán), không phải do cách gọi.

`Cache` là interface thường, nên có thể tự implement nó để làm bản giả trong test hoặc bọc thêm hành vi (ví dụ đếm số lần gọi) quanh một cache thật.

Nguồn trong repo: [CacheException](../src/main/java/app/cache/exception/CacheException.java), [Cache](../src/main/java/app/cache/Cache.java), [module-info](../src/main/java/module-info.java).
