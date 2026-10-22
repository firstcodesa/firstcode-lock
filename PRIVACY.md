# سياسة الخصوصية — قفل الكود الأول

المبرمج: أبوغازي العتيبي • FIRST CODE

- يعمل التطبيق دون إنترنت. لا إعلانات ولا تتبع ولا حسابات ولا إرسال بيانات لخادم.
- يحتفظ محليًا بالتطبيقات التي تختارها وإعدادات اللغة والمظهر والحماية، ومشتق مشفر من الرمز مع ملح عشوائي. لا يحتفظ بالرمز كنص واضح.
- تستخدم خدمة إمكانية الوصول أحداث تغيّر النافذة لمعرفة اسم حزمة التطبيق المفتوح وعرض شاشة قفله. لا تطلب قراءة محتوى النوافذ أو تنفيذ الإيماءات، ولا تقرأ الرسائل أو تسجل ضغطات المفاتيح.
- يجري التحقق بالبصمة في نافذة أندرويد الرسمية؛ لا يتلقى التطبيق صورة البصمة أو قالبها، بل نتيجة التحقق فقط. يبقى رمز التطبيق بديلًا.
- «مسؤول الجهاز» اختياري ويضيف خطوة تعطيله قبل الإزالة. لا يطلب التطبيق سياسات مسح البيانات أو تغيير قفل الجهاز. يستطيع المالك إلغاء تفعيله من إعدادات أندرويد.
- يمكن إيقاف القفل مؤقتًا مع حفظ الاختيارات، أو إلغاء اختيار التطبيقات، أو إلغاء خدمة إمكانية الوصول من الإعدادات.
- لإزالة البيانات: عطّل مسؤول الجهاز إن كان مفعّلًا، ثم أزل التطبيق. لا توجد نسخة بيانات على خوادمنا. النسخ الاحتياطي التلقائي للتطبيق معطّل.
- هذا قفل إضافي للتطبيقات، وليس تشفيرًا لبياناتها أو بديلًا عن قفل شاشة الهاتف. لا يحجب الإشعارات أو صور التطبيقات الأخيرة. لا يمنع المالك من إدارة جهازه، ولا يضمن الحماية على جهاز مخترق أو ذي صلاحية root.

# Privacy policy — FirstCode Lock

Developer: Abu Ghazi Al-Otaibi • FIRST CODE

The app works offline without ads, tracking, accounts, or server uploads. It stores your selected apps, preferences, and a salted PIN derivative locally; never the plain PIN. Accessibility window-change events identify the foreground app package for locking. Window content retrieval and gestures are not requested. Messages and keystrokes are not read or recorded.

Android handles biometric authentication and returns its result; biometric templates and images are not available to this app. Your app PIN remains a fallback. Optional Device Admin adds a deactivation step before uninstalling and requests no wipe or device-lock policies. The owner can deactivate it through Android settings at any time.

Pause locking while keeping selections, clear your selections, or revoke accessibility permission in settings. To remove local data, deactivate Device Admin if enabled and uninstall. There is no server copy; automatic app backups are disabled.

This is additional app access protection, not encryption of other apps or a replacement for the device screen lock. Notifications and recent-app previews are not hidden. It does not prevent the owner from managing the device and cannot guarantee protection on a compromised or rooted device.
