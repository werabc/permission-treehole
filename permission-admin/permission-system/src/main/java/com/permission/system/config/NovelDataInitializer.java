package com.permission.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.permission.common.entity.*;
import com.permission.system.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NovelDataInitializer implements CommandLineRunner {

    private final NovelCategoryMapper categoryMapper;
    private final NovelMapper novelMapper;
    private final NovelChapterMapper chapterMapper;
    private final SysUserMapper userMapper;

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryMapper.selectCount(new LambdaQueryWrapper<>()) > 0) {
            log.info("小说数据已初始化，跳过");
            return;
        }

        log.info("========== 开始初始化小说数据 ==========");

        // 1. Categories
        NovelCategory c1 = createCategory("玄幻奇幻", "东方玄幻、异界大陆", 1);
        NovelCategory c2 = createCategory("武侠仙侠", "古典武侠、现代修真", 2);
        NovelCategory c3 = createCategory("都市生活", "都市情感、职场人生", 3);
        NovelCategory c4 = createCategory("历史军事", "穿越历史、架空历史", 4);
        NovelCategory c5 = createCategory("科幻未来", "星际科幻、未来世界", 5);
        NovelCategory c6 = createCategory("悬疑灵异", "悬疑推理、灵异惊悚", 6);
        log.info("小说分类初始化完成");

        // 2. Find author user (backend user can be an author)
        SysUser author = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "backend"));
        Long authorId = author != null ? author.getId() : 3L;
        String authorName = author != null ? author.getNickname() : "后端开发";

        // 3. Sample novels with chapters
        Novel n1 = createNovel("苍穹破天录", authorId, authorName, c1.getId(),
                "https://img.zcool.cn/community/01d5a65f0c8f11e9a1e26b23987c72.jpg",
                "少年林尘，天生废脉，受尽欺凌。一次意外，获得了上古神魔传承，从此踏上了逆天修行之路。"
                        + "在这片广袤的苍穹大陆上，强者为尊，弱肉强食。林尘凭着一腔热血和不屈的意志，一步步攀登武道巅峰。");

        Novel n2 = createNovel("都市最强医者", authorId, authorName, c3.getId(),
                "https://img.zcool.cn/community/01d5a65f0c8f11e9a1e26b23987c72.jpg",
                "他是隐世神医的唯一传人，一手银针可断生死。回归都市后，他只想低调生活，"
                        + "却被卷入了一个个惊心动魄的事件之中。逆天医术在手，且看他如何在这灯红酒绿的都市中，谱写一段传奇。");

        Novel n3 = createNovel("星际流浪者", authorId, authorName, c5.getId(),
                "https://img.zcool.cn/community/01d5a65f0c8f11e9a1e26b23987c72.jpg",
                "公元2580年，人类已踏入星际殖民时代。主角叶星辰是一名普通的星际快递员，"
                        + "在一次送货途中，意外发现了一个惊天秘密，从此开启了波澜壮阔的星际冒险之旅。");

        log.info("小说初始化完成");

        // 4. Chapters for novel 1
        createChapter(n1.getId(), "第一章 废脉少年", 1,
                "北灵镇，林家。\n\n" +
                "练武场上，一群少年正在进行着每日的修炼。拳风呼啸，劲气四射。\n\n" +
                "\"废物！连最基本的聚气都做不到，还不滚下去！\"教头的一声暴喝，让场中一个瘦弱少年身形一颤。\n\n" +
                "周围的少年们纷纷投来嘲讽的目光。林尘低着头，默默地走出了练武场。\n\n" +
                "这已经是他第七十三次被赶出练武场了。在这个以武为尊的世界里，他这样的人，连普通人都不如。");

        createChapter(n1.getId(), "第二章 意外传承", 2,
                "夜深人静。\n\n林尘独自坐在后山的古树下，仰望着星空。\n\n" +
                "\"为什么？为什么我天生经脉闭塞，不能修炼？\"他紧握双拳，指甲陷进了掌心。\n\n" +
                "突然，一道流星划破天际，直直地朝着他的方向坠落。林尘来不及躲避，只感觉额头一阵剧痛，便失去了知觉。\n\n" +
                "当他再次醒来时，脑海中多出了无数玄奥的信息——上古神魔传承，开！");

        createChapter(n1.getId(), "第三章 初窥门径", 3,
                "\"这...这是...\"林尘感受着体内涌动的力量，震撼得说不出话来。\n\n" +
                "原本闭塞的经脉，此刻如同被河水冲刷过的河道，通畅无比。天地灵气如潮水般涌入他的体内。\n\n" +
                "\"聚气一重天！\"林尘喃喃自语，眼中闪烁着不敢置信的光芒。一夜之间，从废脉到聚气一重天，这简直是奇迹。");

        // Chapters for novel 2
        createChapter(n2.getId(), "第一章 下山", 1,
                "青云山，白云观。\n\n" +
                "\"师父，我...\"\n\n\"不必多言，你已学有所成，该下山历练了。\"白发老道摆了摆手，\"记住，医者仁心，悬壶济世。\"\n\n" +
                "苏铭恭恭敬敬地磕了三个头，起身背起那个破旧的药箱，大步走下山去。他并不知道，等待他的，将是怎样一个波澜壮阔的世界。");

        createChapter(n2.getId(), "第二章 车祸现场", 2,
                "市中心十字路口。\n\n一辆豪华跑车和一辆公交车发生了碰撞，现场一片狼藉。\n\n" +
                "\"让一让，我是医生！\"苏铭挤过人群，看到了一个满身是血的中年男人躺在地上。\n\n" +
                "\"没救了，伤得太重了。\"旁边有人叹息道。\n\n苏铭没有理会，从药箱中取出了银针。\"都退后！\"");

        // Chapters for novel 3
        createChapter(n3.getId(), "第一章 星际快递员", 1,
                "公元2580年，地球联邦，第三星域中转站。\n\n" +
                "\"叶星辰，编号K-7742，今日配送任务：12件，目的地坐标已下发。\"冰冷的机械音在耳边响起。\n\n" +
                "叶星辰打了个哈欠，穿上了那件略显陈旧的快递员制服。他拿起任务单，随意扫了一眼。\n\n" +
                "最后一个快递目的地引起了她的注意——X-99废弃矿星。那是整个星域最危险的地方之一。");

        createChapter(n3.getId(), "第二章 废弃矿星的秘密", 2,
                "X-99废弃矿星。\n\n飞船缓缓降落在布满尘埃的星球表面，四周的探测器显示这里没有任何生命迹象。\n\n" +
                "\"真是见鬼的任务。\"叶星辰低声咒骂着，按照坐标走向一个废弃的矿洞。\n\n" +
                "矿洞深处，一个幽蓝色的光芒在闪烁。当他靠近时，那个光芒突然大盛，将他整个人笼罩其中。");

        log.info("小说章节初始化完成");
        log.info("========== 小说数据初始化完成 ==========");
    }

    private NovelCategory createCategory(String name, String desc, int sort) {
        NovelCategory c = new NovelCategory();
        c.setCategoryName(name);
        c.setCategoryDesc(desc);
        c.setSort(sort);
        c.setStatus(1);
        categoryMapper.insert(c);
        return c;
    }

    private Novel createNovel(String title, Long authorId, String authorName,
                               Long categoryId, String coverUrl, String intro) {
        Novel n = new Novel();
        n.setTitle(title);
        n.setAuthorId(authorId);
        n.setAuthorName(authorName);
        n.setCategoryId(categoryId);
        n.setCoverUrl(coverUrl);
        n.setIntro(intro);
        n.setStatus(1);
        n.setWordCount(0L);
        n.setClickCount(1000L + (long) (Math.random() * 5000));
        n.setLikeCount(100L + (long) (Math.random() * 500));
        novelMapper.insert(n);
        return n;
    }

    private void createChapter(Long novelId, String title, int num, String content) {
        NovelChapter ch = new NovelChapter();
        ch.setNovelId(novelId);
        ch.setChapterTitle(title);
        ch.setChapterNum(num);
        ch.setContent(content);
        ch.setWordCount(content.length());
        ch.setIsFree(1);
        chapterMapper.insert(ch);

        // Update novel word count
        Novel novel = novelMapper.selectById(novelId);
        if (novel != null) {
            long total = chapterMapper.selectList(new LambdaQueryWrapper<NovelChapter>()
                            .eq(NovelChapter::getNovelId, novelId))
                    .stream().mapToLong(c -> c.getWordCount() != null ? c.getWordCount() : 0).sum();
            novel.setWordCount(total);
            novelMapper.updateById(novel);
        }
    }
}
