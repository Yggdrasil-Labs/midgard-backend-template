# Changelog

## [1.1.0](https://github.com/Yggdrasil-Labs/midgard-backend-template/compare/v1.0.0...v1.1.0) (2025-12-08)


### ✨ Features

* 增加db目录 ([04060da](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/04060dac1d8f85a8068f5a3ac6a7a827c33b5eff))


### 🐛 Bug Fixes

* 删除数据库信息 ([c38b2fb](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/c38b2fb4484f74d35fe2cdf0fec8948a2e1423ab))


### 📝 Documentation

* 新增工作流的相关文档 ([36765b3](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/36765b3d3f0f5d71a6f42f4bc6d194545754761d))


### 👷 Continuous Integration

* action的依赖升级 ([83f6fef](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/83f6fef46673d304a15bff637d94ef81e80f721f))
* **create-tag:** 优化检查 release PR是否已合入的逻辑 ([9c1f654](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/9c1f654af496a0e47040907494964bd5bd48ee8e))
* **release-please:** PR就使用默认的格式，也不需要bootstrap-sha记录上次发布点 ([6f535ca](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/6f535ca8c4e28b1f990a9026ed8d8e2403b9571d))
* **release-please:** 不在工作流中写死仓库名 ([a157927](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/a1579271198add469e8fa6613ad6e7b461b3840b))
* **release-please:** 修复没打自定义标签问题 ([2737674](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/273767480d04dbd3302d9f53441e7048029a424b))
* **release-please:** 去掉package-name，PR的title默认会使用当前仓库名 ([320a31d](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/320a31df13ffc9256f8c536244985b3f072be5e0))
* **release-please:** 指定PR的title模板 ([2f8a728](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/2f8a728a9c1b4af891d6b19d1b9b7dbcbd63f1d4))
* **release-please:** 禁用自动创建Github Release/Tag ([f914e42](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/f914e424d562834080c155ecc3570f54b892fd01))
* **release-please:** 简化流水线，修复没打自定义标签问题 ([a69f1de](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/a69f1de7cebe3cd45d4fae4885c9d8d4a3a57899))
* **release-please:** 自动重试打标签 ([b4e2df3](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/b4e2df316135aa34f6475f707718cb5c7c5ea908))
* **release:** Release Note 增加Icon ([805c6d6](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/805c6d67aec754d1ce2115122c1d1e26a6e5c009))
* 简化release please，不需要额外更新revision ([eb56125](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/eb561258bd74eb984ca71d94d56f95ba31f92736))


### 🔧 Miscellaneous Chores

* 修正bootstrap-sha ([4baf74d](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/4baf74dc303125c4f068b0a3a8b11d02502a7728))
* 忽略 application-local.yml ([71e1021](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/71e10211f260026ef7f9f95b1a2de5e62449d80d))
* 校验API依赖升级 ([3702b6a](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/3702b6a994df496435f8284ce6632cfa2c972f28))
* 统一author ([62bb6eb](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/62bb6ebfff443deaa6eb3aa66fa9388bf95f17fc))

## 1.0.0 (2025-12-06)


### Bug Fixes

* add missing import for Application class in TestApplication ([d23e2c2](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/d23e2c2f242285d81cd63aaffc601faf534b9165))
* 修复编译错误 ([0c36ddb](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/0c36ddb381c778a4657df97645dab964fe1c85fe))


### Code Refactoring

* 代码优化 ([45d1e34](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/45d1e3422f51b13b239d67321feb9e8f3ad915d1))
* 优化类命名与分包策略 ([26546ea](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/26546ea61590598180c354b289e79d08532da389))
* 优化类命名与分包策略 ([df08de1](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/df08de1880b85cf766a3147913ff2f420f75aa8f))
* 优化类命名与分包策略，完善package-info与README ([487aea2](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/487aea2a82d0b376d9ac9d51c8781a511d5358d0))


### Tests

* 修复编译错误 ([64695ce](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/64695cedba95cf1804953102eb3915dcc6f6c3eb))


### Build System

* 为 Maven Wrapper 添加可执行权限 ([f0585a6](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/f0585a600209d97bb527b154083507f54e5aed44))
* 编译时自动格式化 ([ec13c1c](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/ec13c1c9ecd8bebd20f704fc77c08eb93df82627))


### Continuous Integration

* **ci:** 修复ci报错 ([05a1eb5](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/05a1eb57d361071e7ef76a4255a23fa0527a18e5))
* **ci:** 修复ci报错 ([72c3807](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/72c3807281bd15144ba66c1ecd6bb2ea205286f7))
* **ci:** 修复ci报错 ([a76e9ad](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/a76e9ad45a733cc34868c0350f2d6564d22dc2ed))
* **workflow:** Add GitHub Actions workflow for syncing labels ([0f52a74](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/0f52a7426ba68a7909a4371e34c24285ea5199ac))
* 新增常用工作流 ([0b91abb](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/0b91abb2e6c8611b0e4b387fa158d1aa33822784))
* 流水线调整 ([ddc26a6](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/ddc26a68854b9747185b64b3924e51ca37e9bbff))
* 跳过单元测试 ([76752bf](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/76752bf7385d09896ed75fd34725ec413b5f8a56))


### Miscellaneous Chores

* 更新README、支持mvnw ([b3deda0](https://github.com/Yggdrasil-Labs/midgard-backend-template/commit/b3deda074628904686bfd764313ea6fe9e9ebe13))
