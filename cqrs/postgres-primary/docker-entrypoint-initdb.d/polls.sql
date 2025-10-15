create sequence OptionEntity_SEQ start with 1 increment by 50;
create sequence PollEntity_SEQ start with 1 increment by 50;
create sequence VoteEntity_SEQ start with 1 increment by 50;

create table OptionEntity (
    id bigint not null,
    pollOption varchar(255),
    primary key (id)
);

create table PollEntity (
    created timestamp(6),
    id bigint not null,
    question varchar(128),
    primary key (id)
);

create table PollEntity_OptionEntity (
    PollEntity_id bigint not null,
    options_id bigint not null unique
);

create table PollEntity_VoteEntity (
    PollEntity_id bigint not null,
    votes_id bigint not null unique
);

create table VoteEntity (
    id bigint not null,
    pollId bigint,
    votedOn timestamp(6),
    votedOption bigint,
    primary key (id)
);

alter table if exists PollEntity_OptionEntity 
    add constraint FK1j6vra5mbe5r184spxq5156vy 
    foreign key (options_id) 
    references OptionEntity;

alter table if exists PollEntity_OptionEntity 
    add constraint FKm69xi9kvycqe0a58x3krw5a09 
    foreign key (PollEntity_id) 
    references PollEntity;

alter table if exists PollEntity_VoteEntity 
    add constraint FKcrl9llrmdunbpagv7sp3l4mf 
    foreign key (votes_id) 
    references VoteEntity;

alter table if exists PollEntity_VoteEntity 
    add constraint FKe3sk61979cn6i5h8ffm9lm7r4 
    foreign key (PollEntity_id) 
    references PollEntity;

alter table OptionEntity replica identity full;
alter table PollEntity replica identity full;
alter table PollEntity_OptionEntity replica identity full;
alter table PollEntity_VoteEntity replica identity full;
alter table VoteEntity replica identity full;

CREATE PUBLICATION poll_pub FOR ALL TABLES;
CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD 'replicator';
GRANT SELECT ON ALL TABLES IN SCHEMA public TO replicator;
