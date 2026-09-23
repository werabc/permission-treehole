-- 前端改版视觉验收用的演示数据（仅用于 e2e 库，不要在生产执行）
SET NAMES utf8mb4;

UPDATE th_user SET nickname = '夜航船', bio = '在第 3 个城市漂着' WHERE id = 1;
UPDATE th_user SET nickname = '林深见鹿', bio = '愿意听，也愿意说' WHERE id = 2;

DELETE FROM th_post WHERE deleted = 0;
DELETE FROM th_comment WHERE deleted = 0;
DELETE FROM th_notification;

INSERT INTO th_post
  (id, user_id, category_id, content, is_anonymous, is_top, status, view_count, like_count, comment_count, create_time) VALUES
(1, 1, 3,  '今天第三次面试挂了。走出写字楼的时候正好下雨，我没有带伞，就那样走了一段。突然觉得这半年像一场很长的梅雨季——但雨总会停的吧，总会的。', 1, 1, 1, 1204,  386, 2, NOW() - INTERVAL 3 MINUTE),
(2, 1, 1,  '妈妈打电话问我最近吃得好不好，我说挺好的。挂掉电话，看着桌上那碗泡面，笑了一下，然后哭了。其实也没有多难，只是突然很想她。', 0, 0, 1,  872,  291, 1, NOW() - INTERVAL 22 MINUTE),
(3, 2, 4,  '考研二战上岸了。没人知道这一年我在图书馆的角落哭过多少次，也没人知道我为什么还笑得出来。我只是不想再让爸妈说“没事，明年再来”。', 1, 0, 1, 2106,  743, 0, NOW() - INTERVAL 1 HOUR),
(4, 2, 2,  '养了 6 年的猫今天走了。它最后是趴在我手边睡着的，很安静，像平时一样。我到现在也没舍得收走它的碗。', 1, 0, 1, 3458, 1209, 0, NOW() - INTERVAL 3 HOUR),
(5, 1, 1,  '暗恋了三年的人今天发了婚礼邀请。我点了“我会去”，然后哭了一整晚。第二天还是挑了一件最好看的裙子。', 1, 0, 1, 1893,  612, 0, NOW() - INTERVAL 8 HOUR),
(6, 2, 5,  '第一次带爸妈出国，他们在机场像小孩一样兴奋，拉着我拍了好多模糊的照片。突然很想努力赚钱，让他们再多看几个地方。', 0, 0, 1,  967,  401, 0, NOW() - INTERVAL 1 DAY),
(7, 1, 4,  '实习三个月，今天第一次被 leader 当众表扬。回家路上给自己买了杯最贵的奶茶，坐在楼下喝完才上楼。', 1, 0, 1,  534,  188, 0, NOW() - INTERVAL 2 DAY);

INSERT INTO th_comment (id, post_id, user_id, content, is_anonymous, like_count, status, create_time) VALUES
(1, 2, 2, '我妈也总问我吃得好不好，我每次都说挺好的。谢谢你把这句话写出来，让我知道不止我一个人这样。', 1, 96, 1, NOW() - INTERVAL 2 HOUR),
(2, 2, 1, '下次可以试着跟她说一句“我有点累”。她可能会心疼，但也会知道你在好好活着。', 0, 142, 1, NOW() - INTERVAL 1 HOUR),
(3, 1, 2, '梅雨季总会过去的。你已经比昨天多撑住了一天，这就够了。', 1, 58, 1, NOW() - INTERVAL 2 MINUTE);

INSERT INTO th_notification (user_id, sender_id, type, target_type, target_id, content, is_read, create_time) VALUES
(1, 2, 'COMMENT',       'POST', 2, '林深见鹿 回响了你的心事《妈妈打电话问我最近吃得好不好》', 0, NOW() - INTERVAL 2 HOUR),
(1, 2, 'LIKE',          'POST', 2, '你的心事收到了 128 次抱抱',                                   0, NOW() - INTERVAL 5 HOUR),
(1, NULL, 'REPORT_RESULT','POST', 2, '系统通知：你的心事已通过审核，现已出现在广场',              0, NOW() - INTERVAL 1 DAY),
(1, NULL, 'LIKE',       'POST', 2, '你的心事被 12 人收藏',                                       1, NOW() - INTERVAL 3 DAY);

UPDATE th_category SET post_count = (SELECT COUNT(*) FROM th_post p WHERE p.category_id = th_category.id AND p.deleted = 0 AND p.status = 1);
