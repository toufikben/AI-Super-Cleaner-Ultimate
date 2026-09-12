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




هذه هي المرحلة النشطة الآن. هدفها تحويل Release Candidate إلى نسخة Internal Testing قابلة للمراجعة.




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




### المتبقي في المرحلة 15




- إنشاء UMP Privacy & messaging message في AdMob Console.
- إضافة Privacy Policy URL عام وثابت إلى AdMob وPlay Console.
- انتظار قبول AdMob account/app ومراجعة Policy Center.
- اختيار/إنشاء Google Play developer app.
- إنشاء تطبيق Play Console بالـpackage name `com.aisupercleaner.ultimate`.
- تأكيد/تفعيل منتج الاشتراك `premium_monthly`.
- إنشاء منتج الشراء مدى الحياة `premium_lifetime` في Play Console (تم الإنشاء؛ يلزم تأكيد السعر والتفعيل).
- إعداد أسعار وبلدان المنتجات.
- إضافة License Tester بحساب Google مخصص للاختبار.
- إعداد Internal Testing track.
- إنشاء AAB موقع باستخدام Secrets آمنة.
- تشغيل Instrumentation على جهاز أو Emulator.
- تنفيذ Billing/UMP/Ads tests على نسخة Internal.
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
