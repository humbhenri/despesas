create table despesas (
       id uuid primary key default gen_random_uuid(),
       created_at timestamp not null default current_timestamp,
       content text not null,
       val numeric(12, 2) not null
)
