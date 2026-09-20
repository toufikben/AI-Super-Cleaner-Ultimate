# AI Super Cleaner Ultimate — خريطة الطريق الحالية

**آخر تحديث:** 20 سبتمبر 2026  
**المصدر المرجعي:** commit `5d4c3d4` (`Harden cleanup vault scheduling and localization`) + الفحص الحي في Google Play Console + الأصول المحلية الجاهزة.

> هذه الخريطة لا تعتمد على خطة قديمة. كل حالة أدناه مبنية على ما ظهر في الكود الحالي أو ما تم التحقق منه مباشرة في Google Play Console. أي عنصر لم يتم التحقق منه لا يُسجل كمكتمل.

## 1. ما تم إنجازه فعليًا في الكود الحالي

| المجال | النتيجة الحالية | الحالة |
|---|---|---|
| هوية التطبيق | Package `com.aisupercleaner.ultimate`، الإصدار `1.0.4`، versionCode `4`، compile/target SDK `36` | مُثبت |
| `QUERY_ALL_PACKAGES` | غير موجود في Manifest أو المصدر الحالي، مع `<queries>` محدد لتطبيقات user-launchable | مُثبت في المصدر الحالي |
| حواجز الإصدار | سكربتا `qa_release.sh` و`verify_release.sh` يتحققان من عدم وجود `QUERY_ALL_PACKAGES` | مُثبت |
| التنظيف | فصل مسارات الحذف الناجح والفاشل وحساب المساحة للنجاح فقط | موجود في commit الحالي |
| Vault/PIN | PBKDF2 مع salt وbackoff وترقية legacy، وملفات preview مؤقتة بأسماء غير مكشوفة | موجود في commit الحالي |
| الجدولة | WorkManager يقرأ الفواصل من DataStore ويستخدم Unique Work/backoff | موجود في commit الحالي |
| مسح البيانات | نتيجة مفصلة لمسح WorkManager وVault وDB وDataStore، مع إبقاء مفاتيح التشفير عمدًا | موجود في commit الحالي |
| اللغات | ملفات locales متساوية في المفاتيح؛ parity العددي لا يثبت جودة الترجمة أو عدم overflow | مُثبت جزئيًا |
| CI | مسار release يبني AAB/APK موقعًا باستخدام GitHub Secrets، دون تخزين الأسرار في الريبو | موجود، يحتاج تشديد gates |

## 2. ما تم إنجازه في Google Play Console أو أصبح مسودة

| العنصر | الحالة التي تم التحقق منها | ملاحظة |
|---|---|---|
| التطبيق المستهدف | تم العمل على **AI Super Cleaner Ultimate فقط**، وليس تطبيقًا آخر | مُثبت |
| النص الإنجليزي | تم إدخال وصف قصير ووصف كامل محسّنين ومبنيين على الخصائص الموجودة | مسودة؛ لم يتم الإرسال للمراجعة |
| النص العربي | موجود داخل locale `Arabic – ar` | موجود كمسودة/محتوى locale؛ لم يتم نشره |
| النص الفرنسي | موجود داخل locale `French (France) – fr-FR` | موجود كمسودة/محتوى locale؛ لم يتم نشره |
| English phone screenshots | توجد 8 صور في المحرر | لم يُثبت استبدالها بالنسخة الجديدة في هذه الجولة |
| Arabic phone screenshots | توجد 8 صور حالية في المحرر | رفع النسخة العربية الجديدة لم يكتمل؛ لا نسجلها كمرفوعة |
| French phone screenshots | توجد 8 صور حالية في المحرر | رفع النسخة الفرنسية الجديدة لم يكتمل؛ لا نسجلها كمرفوعة |
| Feature Graphic | ظهر أصل Feature Graphic في محررات بعض اللغات، والأصل الجديد الجاهز محفوظ محليًا | يجب إعادة التحقق من locale English/Arabic/French قبل اعتبار الاستبدال مكتملًا |
| النشر أو الإرسال للمراجعة | لم يتم الضغط على Submit for review أو Publish | محمي؛ لا إجراء نشر دون موافقة صريحة |

## 3. الأصول الجاهزة محليًا

الأصول التالية جاهزة وتحققت أبعادها ووجودها، لكنها لا تُعد مرفوعة إلى Play Console إلا بعد ظهورها داخل محرر اللغة وعدّها ضمن Phone assets:

- `ar-01.png` إلى `ar-08.png` — 800×1500، نص عربي RTL.
- `fr-01.png` إلى `fr-08.png` — 800×1500، نص فرنسي.
- `en-01.png` إلى `en-08.png` — 800×1500، نص إنجليزي.
- `feature-graphic-1024x500.png` — 1024×500.

المسار المحلي للأصول: `/home/ubuntu/audits/play-assets-improved/`.

## 4. الأولويات التالية — بالترتيب التنفيذي

| الأولوية | العمل | معيار الإغلاق | الحالة |
|---|---|---|---|
| P0 | مطابقة AAB/track الذي ظهر معه رفض `QUERY_ALL_PACKAGES` مع commit `5d4c3d4` | استخراج Manifest من AAB المرفوع أو تأكيد نتيجة Console بعد انتهاء المراجعة | مفتوح |
| P0 | حسم `MANAGE_EXTERNAL_STORAGE` | قرار سياسة موثق: إبقاء مع declaration وإفصاح أو إزالة مع بديل وظيفي | مفتوح |
| P0 | إكمال رفع الأصول العربية والفرنسية والإنجليزية عند الحاجة | ظهور 8 صور الصحيحة لكل locale داخل Console ثم `Save as draft` | متوقف تقنيًا عند native file picker |
| P1 | تشغيل build/tests/lint/AAB validation ببيئة JDK كاملة | نتائج pass/fail قابلة للتكرار، لا فشل بيئي | مفتوح؛ التشغيل السابق توقف قبل compilation بسبب JDK |
| P1 | وصل Quick Clean shortcut أو إزالته | اختبار action `QUICK_CLEAN` على cold start وwarm start | مفتوح |
| P1 | إزالة claims مثل “Military” و“DoD 5220.22-M” من Shredder | UI وlisting يصفان الحذف بأنه best-effort على flash storage | مفتوح |
| P1 | إضافة اختبارات Vault وWorkManager وSAF وpermissions وlifecycle | اختبارات instrumentation تغطي النجاح والفشل والإلغاء وprocess death | مفتوح |
| P1 | نقل النصوص hardcoded إلى resources ومراجعة رسائل الأخطاء | لا نصوص user-facing خارج resources ولا تفاصيل SDK للمستخدم | مفتوح |
| P1 | تدقيق Billing/Ads/UMP وData Safety | entitlement بعد restart/expiry/refund، وconsent قبل الإعلانات، وتطابق السياسة | مفتوح |
| P2 | تدقيق Privacy Scanner على Android 11+ | إثبات أن package visibility المحدودة تحقق الوعد الوظيفي | مفتوح |
| P2 | اختبار backup/restore وClear All Data على Android 12+ | لا تسريب لـVault/DB/الإعدادات الحساسة، ونتيجة واضحة للمستخدم | مفتوح |
| P2 | قياس Store Listing وASO | تجربة واحدة بفرضية وmetric ومدة وقرار، دون ادعاء نتائج مسبقة؛ النص العربي والفرنسي محفوظ كمسودة | مفتوح بعد توفر البيانات |
| P2 | تدقيق Monetization setup والمنتجات | مطابقة `premium_monthly` و`premium_lifetime` مع المنتجات والأسعار والحالات في Console | مفتوح |

## 5. قواعد العمل من الآن

1. لا يتم رفع AAB أو اختيار تطبيق آخر؛ النطاق هو `AI Super Cleaner Ultimate` فقط.
2. لا يتم اعتبار إزالة صلاحية من المصدر مساوية لإزالة الصلاحية من AAB المرفوع حتى تتم مطابقة artifact والـtrack.
3. لا يتم اعتبار ملف محلي أو محاولة upload نجاحًا؛ النجاح يتطلب تحققًا من Console.
4. تعديلات Play Store النصية والصور تحفظ كمسودات فقط ما لم يطلب المالك صراحة الإرسال للمراجعة.
5. أي عائق يتطلب صلاحية خارجية أو نافذة نظام أصلية يُسجل كـ **يحتاج تدخلًا خارجيًا**، وليس كفشل في الفحص أو سبب لتخطي المهمة.
6. لا تُعدل خطة الطريق من وثيقة قديمة دون مقارنة مباشرة مع commit الحالي والكود وConsole.

## 6. قرار التنفيذ الحالي

**المرحلة الحالية: تدقيق listing والإصدار والسياسات قبل النشر.** تم تحديث النصوص والمواد البصرية محليًا، وقرر المالك تجاوز رفع الصور والفيديو والتعامل معه يدويًا. الفحص الحي الأخير يثبت أن App Content مكتمل، لكن Policy status ما زال يعرض رفض `QUERY_ALL_PACKAGES`، والتحديث ما زال **In review**، وتغييرات Production/Open testing وStore listings ضمن **Changes in review**. لا توجد موافقة على Remove changes أو Submit أو Publish.

## 7. Skills وأدوات GitHub التي تم تقييمها

| الأداة | الإجراء | النتيجة |
|---|---|---|
| `blockmatic/basilic@playwright-v1` | تم العثور عليها عبر `npx skills find` ثم تثبيتها وتشغيل Playwright فعليًا | مفيدة للأتمتة العامة ورفع الملفات عندما توجد جلسة مصادق عليها |
| Playwright standalone | فُتح رابط Play Console من جلسة Chromium مستقلة | أعاد Google إلى صفحة Sign in؛ لا يشارك جلسة المتصفح المصادق عليها في موصل Manus |
| Skills catalog | تم البحث عن browser/file-upload/Playwright/Google Play skills | لم يظهر Skill موثوق ومحدد لـGoogle Play Console؛ أقرب نتيجة عملية كانت Playwright |
| Google Play API connector | تم فحص إعدادات connectors | لا يوجد Google Play API أو Service Account مهيأ في هذه الجلسة |

### قرار الأتمتة بعد التجربة

لم يتم تثبيت أي Skill عشوائي خاص برفع الملفات لمجرد اسمه؛ النتائج منخفضة الاستخدام أو لا تتعلق بـGoogle Play. تم استخدام Playwright Skill فعليًا، لكن غياب جلسة OAuth في Chromium المستقل يمنع الوصول إلى حساب Console. المساران التقنيان القابلان للإغلاق لاحقًا هما:

1. استخدام جلسة المتصفح المصادق عليها مع file picker يدوي قصير، ثم التحقق من ظهور الصور داخل Console.
2. تفعيل Google Play Developer API/Service Account Connector، ثم استخدام API أو Fastlane/Supply لرفع metadata والصور دون واجهة المتصفح.

هذه النتيجة ليست تخطيًا ولا فشلًا في فحص التطبيق؛ إنها **حاجة إلى جلسة مصادق عليها أو Connector خارجي**. لم تُحفظ أي بيانات اعتماد داخل المستودع.

## 8. آخر قراءة حية بعد تجاوز الصور والفيديو

التفاصيل الكاملة محفوظة في [PLAY_CONSOLE_LIVE_STATUS_2026-09-20.md](PLAY_CONSOLE_LIVE_STATUS_2026-09-20.md). أهم النتائج: App Content يعرض “You're all caught up”، Android vitals وRatings غير متاحة، Protected with Play يعرض Good protection، وPlay Integrity API غير مدمج (0/7). تم الآن حفظ الوصفين العربي والفرنسي كمسودة مع إبقاء اسم التطبيق كما هو وعدم رفع الصور والفيديو. ما زال يلزم مطابقة الـAAB/track الفعلي مع commit الحالي وحسم `MANAGE_EXTERNAL_STORAGE` قبل اعتبار مشكلة الرفض مغلقة.

## الأدلة المرتبطة

- `play-store-listing.md` — نصوص المتجر الحالية.
- `AI_SUPER_CLEANER_CURRENT_CODE_AND_PLAY_AUDIT.md` — تقرير الفحص التفصيلي.
- `app/src/main/AndroidManifest.xml` — الصلاحيات و`queries` الحالية.
- `scripts/qa_release.sh` و`scripts/verify_release.sh` — حواجز الإصدار.
- الأصول المحلية: `/home/ubuntu/audits/play-assets-improved/`.
- المستودع: `https://github.com/toufikben/AI-Super-Cleaner-Ultimate`.

> **تنبيه:** لا يحتوي هذا الملف على أسرار توقيع أو مفاتيح Google Play أو بيانات اعتماد.
