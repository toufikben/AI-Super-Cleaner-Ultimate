# Google Play Console — الحالة الحية بعد تجاوز الصور والفيديو

**التاريخ:** 20 سبتمبر 2026  
**التطبيق:** AI Super Cleaner Ultimate  
**Package:** `com.aisupercleaner.ultimate`

## الخلاصة

تم تجاوز مرحلة الصور والفيديو بناءً على طلب المالك. لم تُرفع الأصول الجديدة إلى Google Play Console، وسيقوم المالك برفعها يدويًا لاحقًا. استمر الفحص في المراحل الأخرى، والنتيجة الحالية هي أن التطبيق لديه تحديث قيد المراجعة، لكن Play Console ما زالت تعرض رفضًا مرتبطًا بـ`QUERY_ALL_PACKAGES`.

## الحالة الحية المتحققة

| القسم | الحالة الحالية | القرار |
|---|---|---|
| Policy status | **App rejected — QUERY_ALL_PACKAGES permission**، مرفوض في 18 سبتمبر 2026 | لم يُعتبر الإصلاح مكتملًا حتى تختفي المخالفة أو يظهر قرار جديد من Google |
| Review state | **Update in review** | لا نرسل تحديثًا جديدًا ولا نغير مسار المراجعة تلقائيًا |
| App content | صفحة **Need attention** تعرض “You're all caught up” | لا توجد إقرارات App Content معلقة في القراءة الحالية |
| Publishing overview | Production 4 (1.0.4) وOpen testing 4 (1.0.4) وتغييرات Store listings لعدة لغات ضمن **Changes in review** | لا نضغط Remove changes أو Submit أو Publish |
| Store listing scope | English، Arabic، French، Spanish، German، Portuguese، Italian، Turkish، Indonesian، Hindi، Japanese، Korean، Chinese، Russian، Urdu، Chinese Traditional ظاهرة كتغييرات لغة | النصوص قيد المراجعة حسب Console؛ الصور الجديدة خارج النطاق الآن |
| Test and release | صفحة المسارات تعرض Get started لـOpen/Closed/Internal testing، مع وجود تغييرات قيد المراجعة | يجب لاحقًا مطابقة التراكات والـartifact مع AAB الفعلي بدل افتراض أنها مكتملة |
| Android vitals | Crash rate وANR وSlow cold start: **Data unavailable** لآخر 28 يومًا | لا توجد أرقام يمكن استخدامها في ASO أو تقرير جودة |
| Ratings/reviews | Average rating، Total users، Ratings with reviews: `-` | لا توجد بيانات مراجعات قابلة للتحليل حاليًا |
| App integrity | صفحة App integrity نقلت الإعدادات إلى Protected with Play | لا يوجد إجراء مطلوب من الصفحة القديمة |
| Protected with Play | Good protection؛ Automatic protection: 1/1؛ Play Store protection: 6/7؛ Play Billing protection: 4/4؛ Play Integrity API: 0/7 وغير مدمج | Play Integrity API تحسين أمني لاحق، وليس شرطًا لرفع الصور أو إغلاق رفض Query الحالي بحد ذاته |
| Automatic protection | Play installs 100.0%، Unknown redistribution غير متاح/بدون قيمة ظاهرة | لا توجد إشارة إعادة توزيع غير معروفة في البيانات الحالية |

## ما تم تجاوزه حسب طلب المالك

لم يتم رفع أي صورة أو فيديو جديد إلى Play Console. الأصول موجودة في حزمة منفصلة للاستخدام اليدوي، ولا تدخل في قرار جاهزية الإصدار أو policy status.

## الأعمال المتبقية غير المرتبطة بالصور والفيديو

1. مطابقة الـAAB والـtrack الذي رُفض مع commit `5d4c3d4` أو مع الإصدار الفعلي الموجود في Console.
2. التحقق من Manifest المدمج داخل AAB المرفوع، وليس الاكتفاء بغياب `QUERY_ALL_PACKAGES` من المصدر.
3. حسم وضع `MANAGE_EXTERNAL_STORAGE` وتوثيق سبب إبقائه أو إزالته وفق وظيفة التطبيق الأساسية.
4. قراءة تفاصيل Production/Open testing من صفحات التراكات ومطابقة versionCode والـreview state.
5. تشغيل build، unit tests، lint، وAAB validation ببيئة JDK كاملة؛ الفشل السابق كان بيئيًا قبل compilation.
6. تدقيق Quick Clean shortcut، Shredder claims، Vault lifecycle، WorkManager، Billing، UMP، Data Safety، والـprivacy policy مقابل التطبيق الحالي.
7. عدم استخدام Vitals أو Ratings كمواد تسويقية حاليًا لأن البيانات غير متاحة.

## قيود القرار

هذه القراءة تشخيصية ولم تنفذ إزالة تغييرات أو إرسالًا للمراجعة أو نشرًا. لا تُعتبر المخالفة محلولة لمجرد أن المصدر الحالي خالٍ من الصلاحية؛ الإثبات النهائي يتطلب نتيجة Google أو مطابقة artifact/track.

## المصادر المحلية

- `ROADMAP.md`
- `play-store-listing.md`
- `app/src/main/AndroidManifest.xml`
- `scripts/qa_release.sh`
- `scripts/verify_release.sh`
- صفحة Console المحفوظة: `/home/ubuntu/page_texts/play.google.com_console_u_0_developers_7591217685335989001_app_4975230222353380613_policy-center.md`
- صفحة App Content المحفوظة: `/home/ubuntu/page_texts/play.google.com_console_u_0_developers_7591217685335989001_app_4975230222353380613_app-content_overv.md`
