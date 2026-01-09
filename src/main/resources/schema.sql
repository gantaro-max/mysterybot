-- 開発用：既存のテーブルがあれば削除してリセットする
-- (外部キー制約があるため、子テーブルから順に削除します)
DROP TABLE IF EXISTS solved_histories;
DROP TABLE IF EXISTS players;
DROP TABLE IF EXISTS riddles;
DROP TABLE IF EXISTS team_groups;

-- 1. イベント・グループ管理テーブル
CREATE TABLE team_groups (
    group_id VARCHAR(50) PRIMARY KEY, -- 例: 'demo'
    group_name VARCHAR(100) NOT NULL,
    admin_pass VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_random_order BOOLEAN DEFAULT FALSE, -- ランダム出題モード設定
    started_at DATETIME DEFAULT NULL
);

-- 2. 謎・問題マスタテーブル
CREATE TABLE riddles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    group_id VARCHAR(50) NOT NULL,
    stage_no INT NOT NULL,            -- 順番モード時の出題順
    question TEXT NOT NULL,
    answer VARCHAR(255) NOT NULL,
    next_msg TEXT,                    -- 正解時のメッセージ
    image_id INT DEFAULT NULL,
    hint_msg VARCHAR(255) DEFAULT NULL,
    FOREIGN KEY (group_id) REFERENCES team_groups(group_id)
);

-- 3. プレイヤー情報テーブル
CREATE TABLE players (
    id INT AUTO_INCREMENT PRIMARY KEY,
    line_user_id VARCHAR(255) NOT NULL,
    group_id VARCHAR(50) NOT NULL,
    
    current_stage INT DEFAULT 0,      -- 初期値0（名前入力待ち）
    player_name VARCHAR(255),         -- チーム名/個人名
    start_at DATETIME,                -- 開始時刻
    finished_at DATETIME,             -- クリア時刻
    current_riddle_id INT,            -- 現在挑戦中の問題ID（ランダムモード用）
    
    last_active TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES team_groups(group_id),
    UNIQUE (line_user_id, group_id)   -- 1人のユーザーは1つのグループ内で一意
);

-- 4. 回答履歴テーブル
-- 誰がどの問題をクリア済みかを記録する
CREATE TABLE solved_histories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player_id INT NOT NULL,
    riddle_id INT NOT NULL,
    solved_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE, -- プレイヤーが消えたら履歴も消す
    FOREIGN KEY (riddle_id) REFERENCES riddles(id)
);

-- 5. 画像保存用テーブル
CREATE TABLE IF NOT EXISTS riddle_images (
    id INT AUTO_INCREMENT PRIMARY KEY,
    data LONGBLOB,             -- 画像データそのもの
    mime_type VARCHAR(50)      -- 画像形式 (image/png 等)
);

-- 6. 「既成問題（カタログ）」用のテーブル
CREATE TABLE IF NOT EXISTS master_riddles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    question TEXT NOT NULL,
    answer VARCHAR(255) NOT NULL,
    hint_msg VARCHAR(255),
    next_msg TEXT NOT NULL,
    image_id INT,              -- 画像は共有して使う
    category VARCHAR(50) DEFAULT '一般' -- ジャンル分け用（例: 初級, 難問, 結婚式）
);

-- ==========================================
-- 初期データの投入（テスト用）
-- ==========================================

-- デモ用グループを作る (ランダムOFFで作成)
INSERT INTO team_groups (group_id, group_name, admin_pass, is_random_order) VALUES ('demo', 'デモ用謎解きイベント', '1234', FALSE);

-- デモグループ用の謎を3問登録する
INSERT INTO riddles (group_id, stage_no, question, answer, next_msg) 
VALUES 
('demo', 1, 'パンはパンでも食べられないパンは？(カタカナで)', 'フライパン', '正解！次は少し難しくなるよ。'),
('demo', 2, '「1=IC」「2=NI」「3=??」??に入るのは？(ローマ字2文字で)', 'SA', '見事！次が最後の問題だ。'),
('demo', 3, '赤くて丸い、医者いらずの果物は？(漢字で)', '林檎', '全問正解おめでとう！');