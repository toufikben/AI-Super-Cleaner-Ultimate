# خارطة مشروع AI Super Cleaner Ultimate




آخر تحديث: 12 سبتمبر 2026




## الحالة العامة




المشروع في **Release Candidate قابل للبناء**. تم إنشاء منتج الشراء مدى الحياة في Play Console، وتبقى تهيئته النهائية واختبار الشراء عبر حساب License Tester وتوقيع AAB ورفع نسخة Internal Testing.




## سجل المراحل السابقة




| المرحلة | النطاق | الحالة | الدليل أو Commit |
|---|---|---|---|
| 1 | Android foundation وUI/UX والملاحة والهوية | مكتملة | `docs/PHASE_1_COMPLETION.md` |
| 2 | الصلاحيات والخصوصية والبنية المحلية دون MANAGE_EXTERNAL_STORAGE | مكتملة | `docs/PHASE_2_COMPLETION.md` |
| 3 | MediaStore scanner وStorage dashboard وRoom | مكتملة | `docs/PHASE_3_COMPLETION.md` |
| 4 | Smart Cleanup Score وExplainable AI | مكتملة | `docs/PHASE_4_COMPLETION.md` |
| 5 | Duplicate وPerceptual Similarity وتحليل الصور | مكتملة | `docs/PHASE_5_COMPLETION.md` |
| 6 | Safe Cleanup وTrash والاستعادة | مكتملة | `docs/PHASE_6_COMPLETION.md` |
| 7 | الفيديو والملفات الكبيرة والتنزيلات والصوت | مكتملة | `docs/PHASE_7_COMPLETION.md` |
| 8 | ضغط الصور والفيديو وسجل الضغط وتصدير النسخ | مكتملة | `docs/PHASE_8_COMPLETION.md` |
| 9 | Quick Clean وAdvanced Smart Scan | مكتملة | `docs/PHASE_9_COMPLETION.md` |
| 10 | Cache وbounded concurrency واختبارات المكتبات الكبيرة | مكتملة | `docs/PHASE_10_COMPLETION.md` |
| 11 | AdMob وLimited Ads وRewarded Ads الاختيارية | مكتملة ثم حُدثت للإنتاج | `docs/PHASE_11_COMPLETION.md` و`docs/ADMOB_PRODUCTION_SETUP.md` |
| 12 | Google Play Billing وPremium وPaywall | مكتملة برمجياً | `docs/PHASE_12_COMPLETION.md` |
| 13 | UMP Consent وPrivacy Options وPrivacy/Data Safety | مكتملة برمجياً، إعداد Console متبقٍ | `docs/PRIVACY_POLICY.md` و`docs/DATA_SAFETY.md` |
| 14 | Unit/Instrumentation source وQA وRelease وCI | مكتملة محلياً، device execution متبقٍ | `docs/PHASE_14_QA_REPORT.md` |




## المرحلة الحالية: 15 — إعداد الحسابات والإصدار التجريبي




هذه هي المرحلة النشطة الآن. هدفها تحويل Release Candidate إلى نسخة Internal Testing قابلة للمراجعة. تقرير التدقيق البرمجي: `docs/PHASE_15_AUDIT.md`.




### المكتمل في المرحلة 15




- الريبو خاص ومزامن على GitHub.
- إنشاء تطبيق AdMob باسم AI Super Cleaner Ultimate.
- إنشاء AdMob App ID الإنتاجي.
- إنشاء Interstitial Cleanup production unit.
- إنشاء Rewarded Advanced Scan production unit.
- استبدال Test IDs في `AndroidManifest.xml` و`AdManager.kt`.
- تشغيل `./scripts/qa_release.sh` بدون `QA_ALLOW_TEST_ADS` بنجاح.
- رفع التحديث إلى GitHub في Commit `a8e883c`.
- تأكيد أن AdMob account/app قيد المراجعة.
- تدقيق AdMob وUMP وBilling وProduct IDs والصلاحيات وRelease؛ إصلاح Premium gating والتحقق من Product IDs في commit `238dfa7`.




### المتبقي في المرحلة 15




- التحقق من نشر UMP Privacy & messaging message في AdMob Console بعد ربطه بتطبيق الإنتاج.
- التحقق من إدخال Privacy Policy URL العام والثابت في AdMob وPlay Console: `https://toufikben.github.io/AI-Super-Cleaner-Privacy-Policy/`.
- انتظار قبول AdMob account/app ومراجعة Policy Center.
- اختيار/إنشاء Google Play developer app.
- إنشاء تطبيق Play Console بالـpackage name `com.aisupercleaner.ultimate`.
- تأكيد/تفعيل منتج الاشتراك `premium_monthly`.
- إنشاء منتج الشراء مدى الحياة `premium_lifetime` في Play Console (تم الإنشاء؛ يلزم تأكيد السعر والتفعيل).
- إعداد أسعار وبلدان المنتجات.
- إضافة License Tester بحساب Google مخصص للاختبار.
- إعداد Internal Testing track.
- إنشاء AAB موقع باستخدام Secrets آمنة ورفع النسخة الجديدة بعد commit `238dfa7`.
- تشغيل Instrumentation على جهاز أو Emulator.
- تنفيذ Billing/UMP/Ads tests على نسخة Internal الجديدة؛ الاختبارات الآلية ناجحة، والاختبار الميداني ما زال متبقيًا.
- اختبار شراء `premium_lifetime` واستعادة المشتريات بحساب License Tester.
- لا يوجد حالياً نظام حسابات أو تسجيل دخول داخل التطبيق؛ إنشاء حساب اختبار للتطبيق يتطلب أولاً إضافة Backend/Auth، لذلك لا يُنشأ حساب وهمي في هذه المرحلة.
- إدخال Data Safety النهائي في Play Console.




## المرحلة 16 — Device QA وInternal Testing




لا تبدأ إلا بعد إتمام المرحلة 15.




- `connectedDebugAndroidTest` على API 26+.
- اختبار أول تشغيل والموافقة والرفض وتعديل Privacy Options.
- اختبار عدم تحميل الإعلانات قبل الموافقة.
- اختبار Rewarded Advanced Scan.
- اختبار Interstitial cooldown وبعد cleanup فقط.
- اختبار رفض الصلاحيات والصلاحيات الجزئية.
- اختبار Smart Scan وQuick Clean وAdvanced Scan.
- اختبار duplicates وsimilar photos.
- اختبار Trash وRestore وPermanent Delete.
- اختبار ضغط الفيديو والصور وتصدير النسخ.
- اختبار مكتبة 1,000 و10,000 ملف.
- اختبار الإلغاء والذاكرة والمساحة المنخفضة والملف المحذوف أثناء الفحص.
- اختبار Premium monthly وLifetime وrestore/cancel/expiry عبر License Testers.
- تسجيل كل فشل في Issue أو Commit، ثم تحديث هذه الخارطة وعدم إعادة الاختبار الناجح بلا سبب.




## المرحلة 17 — Store Compliance Review




- إكمال Privacy Policy URL.
- إكمال Data Safety طبقاً لـSDKs الفعلية.
- مراجعة App content.
- مراجعة Target API وPermissions declarations.
- تأكيد عدم وجود `MANAGE_EXTERNAL_STORAGE`.
- إكمال Ads declaration.
- إكمال Financial features/subscription disclosures عند طلبها.
- إضافة screenshots وfeature graphic وicon وstore listing.

## سجل تحقق Billing — 12 سبتمبر 2026

- تم تحديث التطبيق إلى Google Play Billing Library 8.0.0 وtarget/compile SDK 36.
- الكود يستخدم `premium_monthly` للاشتراك و`premium_lifetime` للشراء لمرة واحدة.
- تم إنشاء سجل الاشتراك `premium_monthly` في Play Console؛ يلزم تأكيد Base plan `monthly` بسعر 2.99 وتفعيله.
- تم إنشاء `premium_lifetime` في Play Console؛ يلزم تأكيد السعر والتفعيل من صفحة One-time products.
- الخطوة التالية: إضافة حساب Google كـ License Tester، ثم توليد AAB موقّع ورفعه إلى Internal Testing.
- بعد التثبيت من مسار الاختبار: التحقق من ظهور Lifetime في paywall، تنفيذ Test purchase، ثم اختبار Restore/إعادة فتح التطبيق.
- ملاحظة الحسابات: التطبيق الحالي يعمل محلياً ولا يملك تسجيل دخول أو Backend؛ حساب الاختبار المطلوب هنا هو حساب Google في Play Console، وليس حساب مستخدم داخل التطبيق.


## خطة التدقيق والترقية الشاملة — مبنية على النص المرفق

**قاعدة التنفيذ:** لا تُعلَّم أي مرحلة `[x]` إلا بعد تعديل الكود أو إضافة الاختبارات المطلوبة، وتشغيل فحوصات المرحلة، وتسجيل النتيجة في تقرير أو commit. المراحل مرتبة حسب اعتمادها على بعضها، ويمكن تنفيذ مراحل متتالية في دفعة واحدة عندما تكون مستقلة وآمنة. لا تتم متابعة إنشاء أو رفع AAB أثناء المراحل البرمجية؛ يُؤجل بناء AAB النهائي والتحقق من التوقيع إلى مرحلة `18 — Final Verification` فقط.

### المرحلة 0 — خط أساس وتدقيق المستودع `[x]`

- [x] فحص شجرة المستودع كاملة، Kotlin، Gradle، Manifest، الموارد، Room، Compose، الخدمات، Ads، Billing، UMP، R8، GitHub Actions، والاختبارات.
- [x] تشغيل clean build وUnit tests وLint وRelease build وAAB؛ Instrumentation غير منفذ لأن `adb` غير مثبت ولا يوجد جهاز أو Emulator.
- [x] حصر TODO/FIXME والـstubs والـplaceholder والكود الميت والقيم المكررة والاستثناءات العامة والعمل على Main thread ومشاكل lifecycle/coroutines.
- [x] إنشاء تقرير baseline يفرق بين الخلل المؤكد، والتحسين المقترح، والاختبار غير الممكن في البيئة الحالية.
- **Check الإغلاق:** ناجح — فحوصات الكود والاختبارات المحلية و`./scripts/qa_release.sh` و`git diff --check`. تم تنفيذ Release/AAB في خط الأساس فقط لتوثيق الحالة، ولن تتم متابعة AAB مرة أخرى حتى المرحلة النهائية.
- **الدليل:** `docs/PHASE_0_BASELINE_AUDIT.md`، والـcommit التالي.

### المرحلة 1 — نموذج مصدر الحقيقة للتخزين والحالة `[x]`

- [x] تعريف حدود ما يفحصه التطبيق فعليًا، وعدم الادعاء بفحص كامل الجهاز عند فحص MediaStore فقط.
- [x] وضع token موحد لظهور الملف وقواعد reconciliation بين نتيجة الفحص وRoom.
- [x] تحديد قواعد invalidation الأساسية: الفحص الجزئي لا يحذف السجلات، والفحص الكامل الناجح يحذف غير المرئي فقط.
- [x] منع بقاء سجلات الملفات المختفية بعد الفحص الكامل، ومنع حذفها عند فشل MediaStore query.
- **Check الإغلاق:** ناجح — `testDebugUnitTest` و`lintDebug` و`git diff --check`، مع تقرير `docs/PHASE_1_STATE_RECONCILIATION.md`. اختبار MediaStore الفعلي محجوب لغياب الجهاز.

### المرحلة 2 — إعادة هيكلة Storage Scanning وMediaStore `[~] قيد التنفيذ`

- [x] تم تحسين حدود MediaStore query ومعالجة provider failure كخطأ آمن.
- [x] فصل قائمة مصادر MediaStore في `MediaStoreInventory` دون خلطها مع محركات التحليل.
- [x] دعم Images/Videos/Audio، مع توضيح أن النطاق هو Android MediaStore؛ دعم Documents/Archives/APK عبر SAF متبقٍ.
- [~] التعامل مع permission changes وinvalid URI وprovider errors في الكود؛ الاختبار الفعلي للصلاحيات محجوب لغياب الجهاز.
- [x] تحديث رسائل التقدم والنطاق لتوضح مصدر الفحص الحقيقي.
- **Check الإغلاق:** غير مكتمل؛ لا تتحول المرحلة إلى `[x]` حتى تضاف الاختبارات والفصل الوظيفي المطلوب.

### المرحلة 3 — مزامنة MediaStore وRoom `[x]`

- [x] تنفيذ reconciliation: URI الحالية ← مقارنة Room ← حذف السجلات القديمة ← تحديث المتغير ← إدخال الجديد.
- [x] دعم الملفات المحذوفة أو غير المرئية، وتغير الصلاحيات وURI غير الصالحة وفشل provider دون حذف خاطئ.
- [x] استخدام batching وtoken واحد للفحص بدل تحميل كل URI في الذاكرة.
- [x] إضافة فهرس وmigration صريحة وقيود primary key، وإزالة destructive fallback.
- **Check الإغلاق:** ناجح برمجيًا — `testDebugUnitTest` و`lintDebug` و`git diff --check`; سيناريو MediaStore الحقيقي محجوب لغياب الجهاز.

### المرحلة 4 — Cache والتحليل الأساسي الآمن `[x]`

- [x] مراجعة `ScanCachePolicy` باستخدام URI والحجم والوقت وMIME والنوع.
- [x] إبطال hash وperceptual hash وblur وscreenshot عند تغير metadata أو algorithm version.
- [x] إضافة `analysisVersion` وmigration تنظف نتائج التحليل القديمة.
- [x] الحفاظ على cancellation وprogress وstreaming I/O للتحليل.
- **Check الإغلاق:** ناجح — Unit tests لـcache metadata وanalysis version وLint.

### المرحلة 5 — Exact Duplicate Engine `[~] قيد التنفيذ`

- [x] إبقاء grouping بالحجم والنوع كمرشح أول فقط.
- [x] استخدام SHA-256 عبر streaming، وعدم اعتبار الاسم أو الحجم أو الوقت دليل تطابق.
- [x] تجنب إعادة hash للنتائج الحالية، ودعم الملفات غير القابلة للقراءة والتقدم والإلغاء.
- [x] استخراج `DuplicateGrouping` وإضافة unit tests لملفات ذات hash مختلف ومتساوٍ وحساب recoverable bytes.
- [ ] إضافة integration tests عبر ContentResolver بملفات متساوية الحجم بمحتوى مختلف وملفات متطابقة وملف كبير.
- **Check الإغلاق:** جزئي — Unit test لـSHA-256 ناجح؛ اختبار ContentResolver/Mediastore الحقيقي متبقٍ.

### المرحلة 6 — Similarity وBlur وScreenshot `[~] قيد التنفيذ`

- [x] فصل exact duplicate عن similarity باستبعاد URIs الموجودة في exact groups.
- [ ] جعل similarity score لكل زوج/عضو، مع buckets أو pre-filter لتقليل O(n²).
- [x] تحسين blur إلى heuristic باسم “Potentially blurry” مع score/confidence، دون حذف تلقائي.
- [x] تحسين screenshot detection عبر filename/path/MIME مع classification/confidence.
- [x] منع أي حذف تلقائي بناءً على similarity أو blur أو screenshot.
- **Check الإغلاق:** جزئي — unit tests للتصنيف والتجميع ناجحة؛ اختبارات الصور الحقيقية resize/compression/crop/night/portrait متبقية.

### المرحلة 7 — Recommendation وStorage Health `[~] قيد التنفيذ`

- [x] فصل recommendation score عن classification confidence عن storage impact وsafety level.
- [x] إزالة ادعاء أن confidence دليل حذف، وتسميتها review-only/heuristic.
- [x] عدم اعتبار large/old/screenshot وحدها junk.
- [x] فصل Storage Pressure عن Cleanup Potential وHealth Score.
- [~] Check جزئي ناجح — Unit tests للحسابات؛ عرض كل الحقول في UI ومراجعة جهازية متبقيان.

### المرحلة 8 — Cleanup Safety وTrash Reconciliation `[~] قيد التنفيذ`

- [x] التحقق من URI لكل عنصر ومعالجة الملف المتغير أو المفقود وأخطاء الصلاحيات/provider.
- [x] جعل Trash/recoverable deletion هو المسار الافتراضي، ومنع permanent delete التلقائي.
- [x] إضافة `PartialSuccess` وإظهارها في UI بدل رسالة نجاح كلية.
- [x] حماية Room Trash: لا يزال السجل حتى تنجح العملية، ومحاولة restore عند فشل تسجيل Trash.
- [~] Check جزئي — Unit test للنتيجة الجزئية؛ سيناريو 15 عنصرًا يحتاج integration test ميدانيًا.

### المرحلة 9 — Coroutines وPerformance `[~] قيد التنفيذ`

- [x] إبقاء I/O على Dispatchers.IO ودعم structured cancellation و`ensureActive`.
- [x] منع double scan عبر Mutex داخل StorageScanner، مع bounded concurrency وchunking للتحليل.
- [x] استخدام batching وdownsampling وعدم تحميل الصور كاملة للتحليل.
- [ ] اختبار 1k و10k و50k مع قياس الذاكرة والوقت على جهاز.
- **Check الإغلاق:** جزئي — compile/Unit/Lint ناجحة؛ اختبارات الضغط والذاكرة والجهاز متبقية.

### المرحلة 10 — Compose/UI Architecture وUX `[~] قيد التنفيذ`

- [~] تم إدخال `ScanUiState` صريح وفصل حالات Idle/PermissionRequired/Scanning/Analyzing/Success/Error/Cancelled.
- [x] إزالة Boolean flags المتعارضة الأساسية، وتمييز busy/progress من الحالة.
- [x] توضيح حدود الفحص ورسائل النتائج الجزئية، وعرض Storage Pressure وReview Potential منفصلين.
- [ ] تفكيك `MainActivity` ومراجعة RTL/TalkBack/rotation/background يدويًا.
- **Check الإغلاق:** جزئي — compile/Unit/Lint ناجحة؛ Compose/device/accessibility review متبقية.

### المرحلة 11 — Permissions وPrivacy وSecurity `[~] قيد التنفيذ`

- [x] مراجعة صلاحيات Android 13+ وAndroid 14 visual selected وAndroid 12 وأقل، دون إضافة صلاحيات واسعة.
- [x] لا توجد سجلات حساسة أو Log/println/stack traces في كود الإنتاج.
- [x] تدقيق exported components وManifest؛ لا يوجد WebView أو FileProvider أو MANAGE_EXTERNAL_STORAGE.
- [x] ضبط `allowBackup=false` و`usesCleartextTraffic=false`.
- [ ] اختبار permission revoke/partial access وData Safety/Privacy SDK review ميدانيًا.
- **Check الإغلاق:** جزئي — static security scan وcompile/Unit/Lint ناجحة؛ device/privacy-console review متبقية.

### المرحلة 12 — AdMob وUMP وBilling

- [ ] مراجعة initialization/lifecycle/retry/duplicate loading/ad frequency/consent/premium removal.
- [ ] إبقاء الإعلانات اختيارية وعدم عرضها قبل destructive confirmation أو على مسار cleanup الآمن.
- [ ] مراجعة Billing للاتصال، reconnection، pending، restored، already owned، acknowledgement، duplicate callbacks، offline، وentitlement غير المحلي.
- [ ] مراجعة UMP للخصوصية والخيارات وإعلانات non-personalized والمناطق المعنية.
- **Check الإغلاق:** Unit tests للحالات، ثم اختبار فعلي على Internal Testing بحساب License Tester، مع عدم إعلان النجاح دون جهاز.

### المرحلة 13 — Compression وMedia3 وError Model

- [ ] تدقيق الجودة والحجم وEXIF/orientation والتوافق الصوتي/الفيديوي والملفات المؤقتة.
- [ ] دعم cancellation وتنظيف output عند الفشل ومنع overwrite للأصل دون تأكيد.
- [ ] استبدال generic errors بأنواع مثل PermissionDenied/FileGone/ProviderUnavailable/ReadFailed/StorageFull/UnsupportedFormat/BillingUnavailable/AdUnavailable.
- [ ] تحويلها إلى رسائل UI مفهومة دون stack traces.
- **Check الإغلاق:** اختبارات codecs/output corruption/storage full/cancel/file gone، والتحقق من بقاء الأصل دائمًا.

### المرحلة 14 — Release وVersioning وR8 وGitHub Actions

- [ ] مراجعة توافق AGP/Gradle/Kotlin/Compose/Room/Media3/Billing/Ads/UMP دون تحديث عشوائي.
- [ ] تدقيق versionCode الرتيب والمتوافق مع Play، وفصل versionName عنه.
- [ ] اختبار R8 على APK/AAB فعلي مع Room/Billing/Ads/UMP/Media3 وقواعد keep مبررة فقط.
- [ ] مراجعة Workflows وJava/SDK/cache/signing/secrets/artifacts/failure handling، ومنع أسرار الإنتاج في debug.
- **Check الإغلاق:** clean/test/lint/debug/release/bundle، فحص التوقيع وversionCode وManifest والـmapping، ونجاح CI.

### المرحلة 15 — Test Suite شامل

- [ ] Unit tests للـcache والduplicates/similarity/blur/screenshot/recommendations/health.
- [ ] Database tests للإدخال والتحديث والحذف والمزامنة والمigrations وTrash.
- [ ] Instrumentation للpermissions/MediaStore/trash/restore/cleanup/Compose/Billing حيث يمكن.
- [ ] Edge cases: empty/one/100k files، same-size different content، deletion during scan، permission revoke، file change، storage full، cancel، double actions، trash expiry.
- **Check الإغلاق:** كل اختبار جديد يمر، مع تقرير يذكر ما لم يمكن تشغيله لغياب جهاز.

### المرحلة 16 — Product Improvements وAI Claims

- [ ] تنفيذ التحسينات التي تخدم السلامة فعليًا فقط: large-file filters، duplicate “keep best”، side-by-side similarity، screenshot/blur confidence، trash days remaining، scan history، exclusions/protected items، undo، themes/localization.
- [ ] عدم إضافة ML أو cloud AI لمجرد الاسم؛ تسمية الخوارزميات heuristic/smart analysis بصدق، أو توثيق أي on-device model وقياس CPU/RAM/offline.
- **Check الإغلاق:** كل ميزة لها test وUX rationale ولا تزيد مخاطر حذف بيانات المستخدم.

### المرحلة 17 — State Machine وObservability

- [ ] تعريف قواعد invalidation الصريحة بين scan/cache/analysis/recommendation/cleanup/trash/restore/compression/premium.
- [ ] إضافة diagnostic logging آمن: start/completed/cancelled/counts/cache hits/analysis/cleanup/trash/billing/ad state.
- [ ] جعل logging build-type-aware وإزالة أو تقليل التفاصيل الحساسة في Release.
- **Check الإغلاق:** اختبارات السيناريوهات المتقاطعة وسجل تشخيصي لا يحتوي مسارات أو أسماء شخصية بلا ضرورة.

### المرحلة 18 — Final Verification وRelease Gate

- [ ] تشغيل clean build وunit وlint وinstrumentation عند توفر الجهاز وrelease/R8.
- [ ] فحص APK/AAB والصلاحيات وstartup/scan/duplicate/similarity/blur/screenshots/recommendations/cleanup/trash/restore/permanent deletion/ads/premium/denial/cancellation/background/low memory.
- [ ] تحديث تقرير نهائي بالأخطاء الحرجة والمنطقية والمعمارية والأداء والأمان والربح وUX والملفات والاختبارات والقيود والأولويات التالية.
- [ ] عدم إعلان أي ميزة “متحققة” دون تشغيل اختبارها فعليًا.
- **Check الإغلاق:** Release gate checklist كاملة، commit نهائي، وخارطة الطريق محدثة بنتائج كل اختبار.

### سجل تنفيذ الخطة

| المرحلة | الحالة | Commit/الدليل | ملاحظات |
|---|---|---|---|
| 0 | [x] مكتملة | `docs/PHASE_0_BASELINE_AUDIT.md` | Build/Unit/Lint/Release/AAB/QA ناجحة؛ Instrumentation محجوب بيئيًا لغياب adb والجهاز. |
| 1 | [x] مكتملة | `docs/PHASE_1_STATE_RECONCILIATION.md` | Reconciliation وRoom migration وsafe query failure؛ Unit/Lint ناجحة. |
| 2 | [~] قيد التنفيذ | — | تم تنفيذ حماية جزئية في StorageScanner، وباقي الفصل والاختبارات متبقٍ. |
| 3 | [x] مكتملة | `docs/PHASE_2_3_4_IMPLEMENTATION.md` | Reconciliation وRoom migration وbatching؛ Unit/Lint ناجحة. |
| 4 | [x] مكتملة | `docs/PHASE_2_3_4_IMPLEMENTATION.md` | Cache metadata/version invalidation واختبارات جديدة ناجحة. |
| 5 | [~] قيد التنفيذ | `docs/PHASE_5_6_IMPLEMENTATION.md` | Grouping وSHA-256 وunit tests؛ integration tests عبر ContentResolver متبقية. |
| 6 | [~] قيد التنفيذ | `docs/PHASE_5_6_IMPLEMENTATION.md` | تصنيف blur/screenshot وفصل exact عن similar؛ اختبارات الصور الحقيقية وتحسين similarity متبقية. |
| 7 | [~] قيد التنفيذ | `docs/PHASE_7_8_9_IMPLEMENTATION.md` | فصل الدرجات وStorageHealth واختبارات الحدود؛ UI/device review متبقي. |
| 8 | [~] قيد التنفيذ | `docs/PHASE_7_8_9_IMPLEMENTATION.md` | PartialSuccess وTrash safety وأخطاء URI؛ integration scenario متبقٍ. |
| 9 | [~] قيد التنفيذ | `docs/PHASE_7_8_9_IMPLEMENTATION.md` | Mutex وcancellation وbounded work؛ اختبارات الضغط والذاكرة متبقية. |
| 10 | [~] قيد التنفيذ | `docs/PHASE_10_11_IMPLEMENTATION.md` | ScanUiState وUX metrics؛ Compose/accessibility/device review متبقية. |
| 11 | [~] قيد التنفيذ | `docs/PHASE_10_11_IMPLEMENTATION.md` | Manifest/security/log audit ناجحة؛ permission/device/console review متبقية. |
| 12 | [~] قيد التنفيذ | `docs/PHASE_12_REVALIDATION_2026-09-12.md` | Consent gating وBilling reconnect/entitlement تحسنت؛ Gradle/device/Console verification متبقية. |
| 13 | [~] قيد التنفيذ | `docs/PHASE_13_COMPRESSION_REVALIDATION_2026-09-12.md` | Structured errors وEXIF وcodec policies واختبارات أضيفت؛ Gradle/device codec verification متبقية. |
| 14 | [~] قيد التنفيذ | `docs/PHASE_14_QA_REPORT.md` | Clean/Unit/Lint/Debug/Release-R8 وartifact checks وGitHub Actions run `34689216133` نجحت؛ signed AAB وInstrumentation متبقية. |
| 15 | [ ] لم تبدأ | — | — |
| 16 | [ ] لم تبدأ | — | — |
| 17 | [ ] لم تبدأ | — | — |
| 18 | [ ] لم تبدأ | — | — |

**سياسة تحديث الخطة:** عند إنهاء أي مرحلة، تُشغّل فحوصات Check الخاصة بها، ثم تتحول خانتها إلى `[x]` مع إضافة commit وتقرير النتيجة. إذا فشل Check تبقى `[ ]` وتُسجل المشكلة بدل إعلان نجاح غير مثبت.
