# تقرير تدقيق المرحلة 15 — الإعدادات والإصدار التجريبي

**تاريخ التدقيق:** 12 سبتمبر 2026

## نطاق التدقيق

تمت مراجعة تكامل AdMob وUMP، Google Play Billing، Product IDs، Manifest والصلاحيات، إعدادات Release، اختبارات الوحدة، سكربت QA، سياسة الخصوصية، وتجهيز Internal Testing. كما تمت مطابقة بنود المرحلة 15 في `PROJECT_ROADMAP.md` مع الكود والملفات المنشورة.

## نتائج الكود

| المجال | النتيجة | الملاحظات |
|---|---|---|
| AdMob Application ID | مكتمل | App ID الإنتاجي موجود في `AndroidManifest.xml`. |
| Interstitial | مكتمل بعد الإصلاح | يتم تحميله بعد تهيئة AdMob، ويُعرض بعد نجاح نقل الملفات إلى Trash فقط، مع cooldown. |
| Rewarded | مكتمل بعد الإصلاح | اختياري، ويعيد تحميل إعلانًا لاحقًا عند انتهاء العرض أو عدم توفره. |
| UMP | مكتمل برمجيًا | لا تتم تهيئة Mobile Ads قبل `canRequestAds()`. يتطلب نجاح إعداد النماذج في AdMob Console. |
| Premium gating | مكتمل بعد الإصلاح | أزرار Rewarded وInterstitial لا تعمل لمستخدم Premium، ويتم تفريغ الإعلانات المحملة عند تفعيل Premium. |
| Product IDs | مكتمل | الكود يستخدم `premium_monthly` و`premium_lifetime` حرفيًا. |
| التحقق من الشراء | مكتمل بعد الإصلاح | لا يُفعّل Premium إلا لمنتجات معروفة وبحالة `PURCHASED`. |
| Pending/Cancelled/Error | مكتمل | توجد رسائل واضحة للحالات المعلّقة، الملغاة، والفاشلة. |
| Acknowledgement | مكتمل | تتم معالجة `acknowledgePurchase` مع الإبلاغ عن الفشل. |
| Restore purchases | مكتمل برمجيًا | تتم مزامنة INAPP وSUBS عند نجاح اتصال Billing. |
| إعادة اتصال Billing | مكتمل بعد الإصلاح | توجد إعادة محاولة بعد انقطاع خدمة Google Play. |
| الصلاحيات | مكتمل | لا يوجد تصريح فعلي لـ`MANAGE_EXTERNAL_STORAGE`; الصلاحيات مقتصرة على MediaStore المطلوبة. |
| Release hardening | مكتمل | Minify وresource shrinking وProGuard مفعّلة، مع دعم توقيع عبر متغيرات بيئة آمنة. |

## إصلاح مهم

قبل هذا التدقيق، كانت دالة معالجة المشتريات تجعل أي عملية شراء بحالة `PURCHASED` تفعّل Premium دون التحقق من Product ID. تم إصلاح ذلك بتصفية المشتريات إلى `premium_monthly` و`premium_lifetime` فقط.

## حالة المرحلة 15 خارج الكود

توجد سياسة خصوصية عامة منشورة على:

<https://toufikben.github.io/AI-Super-Cleaner-Privacy-Policy/>

وتوجد مستودعات GitHub الخاصة والعامة الخاصة بالسياسة. تم تجهيز UMP في AdMob بحسب الإعدادات التي نفذها المستخدم، لكن حالة الحساب والموافقة النهائية تعتمد على معالجة Google ومراجعة الحساب.

تم إنشاء تطبيق Play Console ومنتجات Billing ومسار Internal Testing بحسب سجل المشروع. يلزم رفع **AAB جديد موقّع** بعد آخر إصلاحات هذا التدقيق؛ النسخة السابقة في Internal Testing لا تكفي لإثبات الاختبارات على الكود الحالي.

## حدود التحقق

تم التحقق آليًا من Unit tests وLint وDebug/Release build. لم يكن هناك جهاز Android أو Emulator متصل، لذلك لم يتم إثبات نافذة Google Play Billing أو عرض الإعلانات أو سلوك UMP على جهاز فعلي. هذه الخطوات تتطلب تثبيت AAB من رابط Internal Testing واستخدام حساب License Tester.

## قرار التدقيق

الكود أصبح **جاهزًا للبناء والإصدار التجريبي**، وليس مكتمل التحقق الميداني بعد. المخرج التالي هو توقيع AAB الجديد ورفعه إلى Internal Testing، ثم تنفيذ مصفوفة اختبار Ads/UMP/Billing على جهاز Android فعلي.
