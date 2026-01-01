-- 開発用：既存のテーブルがあれば削除してリセットする
DROP TABLE IF EXISTS progress;
DROP TABLE IF EXISTS players;
DROP TABLE IF EXISTS riddles;
DROP TABLE IF EXISTS team_groups;

-- 1. イベント・グループ管理テーブル
CREATE TABLE team_groups (
    group_id VARCHAR(50) PRIMARY KEY, -- 例: 'wedding_2024'
    group_name VARCHAR(100) NOT NULL,
    admin_pass VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 謎・問題マスタテーブル
CREATE TABLE riddles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    group_id VARCHAR(50) NOT NULL,
    stage_no INT NOT NULL,            -- 第何問目か
    question TEXT NOT NULL,
    answer VARCHAR(255) NOT NULL,
    next_msg TEXT,                    -- 正解時のメッセージ
    FOREIGN KEY (group_id) REFERENCES team_groups(group_id)
);

-- 3. プレイヤー情報テーブル
CREATE TABLE players (
    id INT AUTO_INCREMENT PRIMARY KEY,
    line_user_id VARCHAR(255) NOT NULL,
    group_id VARCHAR(50) NOT NULL,
    current_stage INT DEFAULT 1,      -- 現在挑戦中のステージ
    last_active TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES team_groups(group_id),
    UNIQUE (line_user_id, group_id)   -- 1人のユーザーは1つのグループ内で一意
);

-- 4. 回答履歴・ログテーブル
CREATE TABLE progress (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player_id INT NOT NULL,
    riddle_id INT NOT NULL,
    is_cleared BOOLEAN DEFAULT FALSE,
    cleared_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player_id) REFERENCES players(id),
    FOREIGN KEY (riddle_id) REFERENCES riddles(id)
);

-- 初期データの投入（テスト用）
-- まずグループを作る
INSERT INTO team_groups (group_id, group_name) VALUES ('demo', 'デモ用謎解きイベント');

-- デモグループ用の謎を3問登録する
INSERT INTO riddles (group_id, stage_no, question, answer, next_msg) 
VALUES 
('demo', 1, 'パンはパンでも食べられないパンは？(カタカナで)', 'フライパン', '正解！次は少し難しくなるよ。'),
('demo', 2, '「1=IC」「2=NI」「3=??」??に入るのは？(ローマ字2文字で)', 'SA', '見事！次が最後の問題だ。'),
('demo', 3, '赤くて丸い、医者いらずの果物は？(漢字で)', '林檎', '全問正解おめでとう！');