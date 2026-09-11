# خارطة مشروع AI Super Cleaner Ultimate

آخر تحديث: 11 سبتمبر 2026

## الحالة العامة

المشروع في **Release Candidate قابل للبناء**. المستودع خاص على GitHub، وآخر تحديثات AdMob الإنتاجية مرفوعة. لم يتم رفع AAB إلى Google Play بعد، ولم يبدأ النشر العام.

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
- إنشاء Billing products بالمعرفين `premium_monthly` و`premium_lifetime`.
- إعداد أسعار وبلدان المنتجات.
- إضافة License Testers.
- إعداد Internal Testing track.
- إنشاء AAB موقع باستخدام Secrets آمنة.
- تشغيل Instrumentation على جهاز أو Emulator.
- تنفيذ Billing/UMP/Ads tests على نسخة Internal.
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
- فحص Content rating.
- إعداد countries/regions وpricing.
- مراجعة Crash/ANR وPlay pre-launch report.

## المرحلة 18 — Closed Testing ثم Production

- إصلاح نتائج Internal Testing.
- الانتقال إلى Closed Testing عند جاهزية المتطلبات.
- مراقبة crashes وANR وconsent وbilling وad policy.
- إعداد staged rollout بنسبة صغيرة.
- عدم تنفيذ Production rollout قبل موافقة المستخدم النهائية على الإصدار العام.
- مراقبة الأداء والإعلانات والشكاوى بعد الإطلاق.

## قواعد عدم تكرار العمل

- كل بند ينتقل إلى `مكتمل` فقط بعد دليل قابل للتحقق.
- كل إصلاح يرفع في Commit مستقل أو مع رسالة واضحة.
- كل نتيجة جهاز تحفظ في تقرير QA مع التاريخ وإصدار التطبيق والجهاز.
- لا نعيد أي اختبار ناجح إلا إذا تغير الكود أو البيئة أو ظهر عطل مرتبط.
- لا نعتبر إعداداً في Console مكتملاً بناءً على النية؛ يجب وجود شاشة نجاح أو حالة محفوظة.
- بعد كل تحديث، يحدث هذا الملف و`PRE_RELEASE_CHECKLIST.md` قبل رفع Commit.

## آخر نتائج التحقق

- `QA_ALLOW_TEST_ADS=1 ./scripts/qa_release.sh`: ناجح سابقاً للتطوير.
- `./scripts/qa_release.sh`: ناجح بعد استبدال IDs الإنتاجية.
- `assembleDebug`: ناجح.
- `lintDebug`: ناجح.
- `assembleRelease`: ناجح، وأنتج APK غير موقع.
- Instrumentation execution: لم يُنفذ بعد لعدم وجود جهاز/Emulator متصل في بيئة البناء.

## الحالة التي تنتظر إجراء المستخدم

- تسجيل الدخول أو اختيار حساب Google Play Console عند ظهور CAPTCHA أو إعادة المصادقة.
- أسرار التوقيع في GitHub/بيئة إصدار آمنة.
- رابط Privacy Policy العام.
- اختبار الجهاز أو Emulator.
- License Tester purchases.
- الموافقة النهائية قبل أي Production rollout.
