
一、stamp表情包括image/anime两大类，显示时根据stampconfig.plist解析结果显示。

二、stamp子目录结构解释：
    1.anime
        此目录下的目录结构为：
        |--anime
                |--1_anime                          // 此目录为某一类别的anime，其中"1_anime"为此类anime的名字
                        |--coloredpackimages        // 此类anime被选中时的显示图标
                        |--contents                 // anime内容
                        |--firstframe               // anime的第一帧
                        |--packimages               // 此类anime未被选中时的显示图标
                        |--thumbs                   // 缩略图
                        |--packageinfo.json         // 图片的显示尺寸的json文件
                |--2_anime                          // 此目录为某一类别的anime，其中"2_anime"为此类anime的名字，子目录结构和"1_anime"一样
    2.image
        此目录下的目录结构为：
        |--image
                |--1_image                          // 此目录为某一类别的image，其中"1_image"为此类image的名字
                        |--coloredpackimages        // 此类image被选中时的显示图标
                        |--contents                 // image内容
                        |--packimages               // 此类image未被选中时的显示图标
                        |--thumbs                   // 缩略图
                        |--packageinfo.json         // 图片的显示尺寸的json文件
                |--2_image                          // 此目录为某一类别的image，其中"2_image"为此类image的名字，子目录结构和"1_image"一样

三、stampconfig.plist正文结构解释如下：

<plist version="1.0">
---------------------------------------------
    //1.version对应节点，全部包含在<dict>节点中,
        标识stampconfig.plist的版本
---------------------------------------------
    <dict>
        <key>version</key>
        <string>1.0.0</string>
    </dict>

---------------------------------------------
    //2.image对应节点，即image类型的stamp
        <dict>的每组字节点<key><dict>代表一类image，
        其中<key>标识此类image在整个image类别列表中的排列位置，
        代码中对应listView的item的position
---------------------------------------------
    <key>image</key>
    <dict>
        <key>0</key>
        <dict>
            <key>packid</key>
            <string>1</string>
            <key>packname</key>
            <string>1_img</string>
        </dict>
        <key>1</key>
        <dict>
            <key>packid</key>
            <string>2</string>
            <key>packname</key>
            <string>2_png</string>
        </dict>
    </dict>

---------------------------------------------
    //3.anime对应节点，即anime类型的stamp
        <dict>的每组字节点<key><dict>代表一类anime，
        其中<key>标识此类anime在整个anime类别列表中的排列位置，
        代码中对应listView的item的position
---------------------------------------------
    <key>anime</key>
    <dict>
        <key>0</key>
        <dict>
            <key>packid</key>
            <string>3</string>
            <key>packname</key>
            <string>1_anime</string>
        </dict>
        <key>1</key>
        <dict>
            <key>packid</key>
            <string>4</string>
            <key>packname</key>
            <string>2_anime</string>
        </dict>
    </dict>
</plist>