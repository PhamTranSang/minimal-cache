module minimal.cache.main {
    requires org.slf4j;

    exports app.cache;
    exports app.cache.config;
    exports app.cache.eviction;
    exports app.cache.exception;
    exports app.cache.expiration;
}
