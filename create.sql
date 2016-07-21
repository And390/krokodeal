set names 'utf8';

drop table if exists task_message;
drop table if exists master_task;
drop table if exists task;
drop table if exists feedback;
drop table if exists master_device;
drop table if exists master_info;
drop table if exists user;
drop table if exists wallet_type;
drop table if exists device_type;


create table user (
  id int primary key auto_increment,
  login varchar(255) not null,
  password binary(16) not null,
  role enum('admin','master'),
  created timestamp not null default current_timestamp,

  unique index login_idx (login)
)
character set utf8;

create table wallet_type (
  id int primary key auto_increment,
  title varchar(1000) not null
)
character set utf8;

create table device_type (
  id int primary key auto_increment,
  title varchar(1000) not null,
  type enum('phone','tablet','desktop') not null
)
character set utf8;

create table master_info (
  user_id int not null,
  city varchar(1000),
  phone varchar(30),
  male bit,
  age tinyint,
  wallet_id int,
  wallet_num varchar(100),

  foreign key (user_id) references user(id),
  foreign key (wallet_id) references wallet_type(id)
)
character set utf8;

create table master_device (
  user_id int not null,
  device_id int not null,

  foreign key (user_id) references user(id),
  foreign key (device_id) references device_type(id)
);

create table feedback (
  id int primary key auto_increment,
  user_id int not null,
  subject varchar(1000) not null,
  content text not null,
  created timestamp not null default current_timestamp,

  foreign key (user_id) references user(id)
)
character set utf8;

create table task_type (
  id int primary key auto_increment,
  title varchar(1000) not null
)
character set utf8;

create table task (
  id int primary key auto_increment,
  type_id int null,
  device_id int not null,
  price int not null,
  count_limit int not null default 0,
  time_limit int not null default 0,
  title varchar(1000) not null,
  description text not null,
  status enum('stop', 'start', 'close') not null default 'stop',
  created timestamp not null default current_timestamp,
  started timestamp null,
  stopped timestamp null,
  closed  timestamp null,
  creator_id int not null,

  foreign key (type_id) references task_type(id),
  foreign key (device_id) references device_type(id),
  foreign key (creator_id) references user(id)
  ,index (created)
  ,index (started)
  #,index (stopped)
)
character set utf8;

create table master_task (
  task_id int not null,
  master_id int not null,
  confirmer_id int null,
  status enum('work', 'complete', 'confirm') not null default 'work',
  taken timestamp not null default current_timestamp,
  completed timestamp null,
  confirmed timestamp null,

  primary key (task_id, master_id),
  foreign key (task_id) references task(id),
  foreign key (master_id) references user(id),
  foreign key (confirmer_id) references user(id)
  ,index(taken)
  ,index(completed)
  ,index(confirmed)
);

create table task_message (
  id int primary key auto_increment,
  task_id int not null,
  master_id int not null,
  author_id int not null,
  type enum('text','ico','bmp','gif','jpg','png','tiff','tga'),
  content longblob not null,
  created timestamp not null default current_timestamp,

  foreign key (task_id) references task(id),
  foreign key (master_id) references user(id),
  foreign key (author_id) references user(id)
)
character set utf8;


insert into wallet_type(title) values('WebMoney');
insert into wallet_type(title) values('Яндекс.Деньги');
insert into wallet_type(title) values('QIWI');
insert into wallet_type(title) values('телефон');

insert into device_type(title,type) values('телефон Android','phone');
insert into device_type(title,type) values('iPhone','phone');
insert into device_type(title,type) values('Windows Phone / Mobile','phone');
insert into device_type(title,type) values('планшет Android','tablet');
insert into device_type(title,type) values('iPad','tablet');
insert into device_type(title,type) values('планшет iOS','tablet');
insert into device_type(title,type) values('Windows','desktop');
