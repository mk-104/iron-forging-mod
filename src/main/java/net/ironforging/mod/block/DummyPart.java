package net.ironforging.mod.block;

import net.minecraft.util.StringRepresentable;

/**
 * 5x5x5マルチブロック内のダミーブロックの種類（パーツ部位）を表す列挙型。
 * 重厚な金属フレーム、ガラス窓、溶岩プール、溶岩柱、スポナー、
 * および溶岩で溶かされるアイアンゴーレムの各部位を表現します。
 */
public enum DummyPart implements StringRepresentable {
    FRAME_CORNER("frame_corner"),   // 四隅の垂直金属フレーム柱
    FRAME_BEAM("frame_beam"),       // 上下の水平金属フレーム梁
    GLASS_WALL("glass_wall"),       // 外壁のクリアガラス窓
    TOP_GRATE("top_grate"),         // 天井中央の排気グリル（スモークベント）
    LAVA_FLOOR("lava_floor"),       // 内部底面の一面の溶岩プール
    LAVA_PILLAR("lava_pillar"),     // 内部四隅の溶岩柱
    LAVA_NOZZLE("lava_nozzle"),     // 内部四隅上部の溶岩噴出口ノズル
    SPAWNER("spawner"),             // 内部中央空中のモンスタースポナー
    GOLEM_MELTING("golem_melting"), // 内部溶岩で溶かされるアイアンゴーレム
    FIRE("fire"),                   // 内部最下段の炎
    MOLD("mold"),                   // 内部最下段中心のインゴット金型
    BIG_GLASS_Z("big_glass_z"),     // 前後面中央の3x3特大1枚ガラス窓
    BIG_GLASS_X("big_glass_x"),     // 左右面中央の3x3特大1枚ガラス窓
    FUEL_PORT("fuel_port"),         // ホッパー接続用燃料投入口（石炭イラスト）
    OUTPUT_PORT("output_port");     // ホッパー接続用鉄取出口（鉄インゴットイラスト）

    private final String name;

    DummyPart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
