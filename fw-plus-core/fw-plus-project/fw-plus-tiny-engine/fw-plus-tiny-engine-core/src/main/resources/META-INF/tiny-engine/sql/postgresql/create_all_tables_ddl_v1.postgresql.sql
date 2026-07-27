drop table if exists t_platform;

create table t_platform
(
    id                   serial not null,
    name                 varchar(255) not null,
    published            smallint,
    last_build_info      text,
    description          varchar(2000),
    latest_version       varchar(255),
    latest_history_id    int,
    material_history_id  int,
    image_url            varchar(255),
    sort_plugins         text,
    sort_toolbar         text,
    is_default           smallint,
    prettier_opts        text,
    set_default_by       varchar(60),
    app_extend_config    text,
    data_hash            varchar(255),
    business_category_id int,
    theme_id             int,
    platform_url         varchar(255),
    vscode_url           varchar(255),
    tenant_id            varchar(60),
    renter_id            varchar(60),
    site_id              varchar(60),
    created_by           varchar(60)  not null,
    last_updated_by      varchar(60)  not null,
    created_time         timestamp    not null default current_timestamp,
    last_updated_time    timestamp    not null default current_timestamp,
    primary key (id),
    constraint u_idx_platform unique (tenant_id, name)
);

drop table if exists t_platform_history;

create table t_platform_history
(
    id                  serial not null,
    ref_id              int          not null,
    version             varchar(255) not null,
    name                varchar(255) not null,
    publish_url         varchar(255),
    description         varchar(2000),
    vscode_url          varchar(255),
    material_history_id int          not null,
    sub_count           int,
    material_pkg_name   varchar(255),
    material_version    varchar(255),
    image_url           varchar(255),
    tenant_id           varchar(60),
    renter_id           varchar(60),
    site_id             varchar(60),
    created_by          varchar(60)  not null,
    last_updated_by     varchar(60)  not null,
    created_time        timestamp    not null default current_timestamp,
    last_updated_time   timestamp    not null default current_timestamp,
    primary key (id),
    constraint u_idx_platform_history unique (ref_id, version)
);

drop table if exists t_app;

create table t_app
(
    id                  serial not null,
    name                varchar(255) not null,
    platform_id         int          not null,
    platform_history_id int,
    publish_url         varchar(255),
    editor_url          varchar(255),
    visit_url           varchar(255),
    image_url           varchar(255),
    assets_url          varchar(255),
    state               int,
    published           smallint,
    home_page_id        int,
    app_website         varchar(255),
    css                 text,
    config              text,
    constants           text,
    data_handler        text,
    latest              varchar(255),
    git_group           varchar(255),
    project_name        varchar(255),
    branch              varchar(255),
    is_demo             smallint,
    is_default          smallint,
    template_type       varchar(255),
    set_template_time   timestamp,
    description         varchar(2000),
    set_template_by     varchar(60),
    set_default_by      varchar(60),
    framework           varchar(255),
    global_state        text,
    default_lang        varchar(255),
    extend_config       text,
    data_hash           varchar(255),
    can_associate       smallint,
    data_source_global  text,
    tenant_id           varchar(60),
    renter_id           varchar(60),
    site_id             varchar(60),
    created_by          varchar(60)  not null,
    last_updated_by     varchar(60)  not null,
    created_time        timestamp    not null default current_timestamp,
    last_updated_time   timestamp    not null default current_timestamp,
    primary key (id),
    constraint u_idx_app unique (tenant_id, platform_id, name)
);

drop table if exists t_app_extension;

create table t_app_extension
(
    id                serial not null,
    name              varchar(255) not null,
    type              varchar(255) not null,
    content           text     not null,
    app_id            int          not null,
    category          varchar(255) not null,
    tenant_id         varchar(60),
    renter_id         varchar(60),
    site_id           varchar(60),
    created_by        varchar(60)  not null,
    last_updated_by   varchar(60)  not null,
    created_time      timestamp    not null default current_timestamp,
    last_updated_time timestamp    not null default current_timestamp,
    primary key (id),
    constraint u_idx_app_extension unique (app_id, name)
);

drop table if exists t_block_group;

create table t_block_group
(
    id                serial not null,
    name              varchar(255) not null,
    description       varchar(2000),
    app_id            int          not null,
    platform_id       int          not null,
    tenant_id         varchar(60),
    renter_id         varchar(60),
    site_id           varchar(60),
    created_by        varchar(60)  not null,
    created_time      timestamp    not null default current_timestamp,
    last_updated_by   varchar(60)  not null,
    last_updated_time timestamp    not null default current_timestamp,
    primary key (id),
    constraint u_idx_block_group unique (tenant_id, platform_id, name)
);

drop table if exists t_business_category;

create table t_business_category
(
    id                serial not null,
    code              varchar(255) not null,
    name              varchar(255) not null,
    tenant_id         varchar(60),
    renter_id         varchar(60),
    site_id           varchar(60),
    created_by        varchar(60)  not null,
    created_time      timestamp    not null default current_timestamp,
    last_updated_by   varchar(60)  not null,
    last_updated_time timestamp    not null default current_timestamp,
    primary key (id),
    constraint u_idx_business_category unique (tenant_id, code)
);



drop table if exists t_block;

create table t_block
(
    id                serial not null,
    label             varchar(255) not null,
    name              varchar(255),
    framework         varchar(255),
    content           text,
    assets            text,
    last_build_info   text,
    description       varchar(2000),
    tags              text,
    latest_version    varchar(255),
    latest_history_id int,
    screenshot        text,
    path              varchar(255),
    occupier_by       varchar(60),
    is_official       smallint,
    public            int,
    is_default        smallint,
    tiny_reserved     smallint,
    npm_name          varchar(255),
    i18n              text NULL,
    platform_id       int          not null,
    app_id            int          not null,
    content_blocks    text,
    tenant_id         varchar(60),
    renter_id         varchar(60),
    site_id           varchar(60),
    created_by        varchar(60)  not null,
    created_time      timestamp    not null default current_timestamp,
    last_updated_by   varchar(60)  not null,
    last_updated_time timestamp    not null default current_timestamp,
    primary key (id),
    constraint u_idx_block unique (platform_id, label, framework)
);

drop table if exists t_block_history;

create table t_block_history
(
    id                serial not null,
    ref_id            int          not null,
    version           varchar(255) not null,
    message           varchar(255),
    label             varchar(255) not null,
    name              varchar(255),
    framework         varchar(255),
    content           text,
    assets            text,
    build_info        text,
    screenshot        text,
    path              varchar(255),
    description       varchar(2000),
    tags              text,
    is_official       smallint,
    public            int,
    is_default        smallint,
    tiny_reserved     smallint,
    mode              varchar(255),
    platform_id       int          not null,
    app_id            int          not null,
    npm_name          varchar(255),
    i18n              text NULL,
    content_blocks    text,
    tenant_id         varchar(60),
    renter_id         varchar(60),
    site_id           varchar(60),
    created_by        varchar(60)  not null,
    created_time      timestamp    not null default current_timestamp,
    last_updated_by   varchar(60)  not null,
    last_updated_time timestamp    not null default current_timestamp,
    primary key (id),
    constraint u_idx_block_history unique (app_id, ref_id, version)
);

drop table if exists t_material;

create table t_material
(
    id                      serial not null,
    name                    varchar(255) not null,
    npm_name                varchar(255),
    framework               varchar(255) not null,
    assets_url              text,
    image_url               varchar(255),
    published               smallint,
    latest_version          varchar(255),
    latest_history_id       int,
    public                  int,
    last_build_info         text,
    description             varchar(2000),
    is_official             smallint,
    is_default              smallint,
    tiny_reserved           smallint,
    component_library_id    int,
    material_category_id    int,
    material_size           int,
    tgz_url                 varchar(255),
    unzip_tgz_root_path_url text,
    unzip_tgz_files         text,
    tenant_id               varchar(60),
    renter_id               varchar(60),
    site_id                 varchar(60),
    created_by              varchar(60)  not null,
    created_time            timestamp    not null default current_timestamp,
    last_updated_by         varchar(60)  not null,
    last_updated_time       timestamp    not null default current_timestamp,
    primary key (id),
    constraint u_idx_material unique (tenant_id, name, framework)
);

drop table if exists t_material_history;

create table t_material_history
(
    id                      serial not null,
    ref_id                  int          not null,
    version                 varchar(255) not null,
    content                 text,
    name                    varchar(255) not null,
    npm_name                varchar(255),
    framework               varchar(255),
    assets_url              text,
    image_url               varchar(255),
    build_info              text,
    description             varchar(2000),
    material_size           int,
    tgz_url                 varchar(255),
    unzip_tgz_root_path_url text,
    unzip_tgz_files         text,
    tenant_id               varchar(60),
    renter_id               varchar(60),
    site_id                 varchar(60),
    created_by              varchar(60)  not null,
    created_time            timestamp    not null default current_timestamp,
    last_updated_by         varchar(60)  not null,
    last_updated_time       timestamp    not null default current_timestamp,
    primary key (id),
    constraint u_idx_material_history unique (tenant_id, ref_id, version)
);

drop table if exists t_page;

create table t_page
(
    id                serial not null,
    name              varchar(255) not null,
    app_id            int          not null,
    route             varchar(255) not null,
    page_content      text,
    is_body           smallint,
    parent_id         int          not null,
    "group"           varchar(255),
    depth             int,
    is_page           smallint not null,
    occupier_by       varchar(60),
    is_default        smallint not null,
    content_blocks    text,
    latest_version    varchar(255),
    latest_history_id int,
    tenant_id         varchar(60),
    renter_id         varchar(60),
    site_id           varchar(60),
    created_by        varchar(60)  not null,
    created_time      timestamp    not null default current_timestamp,
    last_updated_by   varchar(60)  not null,
    last_updated_time timestamp    not null default current_timestamp,
    primary key (id),
    constraint u_idx_page unique (app_id,parent_id,route,is_page,tenant_id, name)
);

drop table if exists t_page_history;

create table t_page_history
(
    id                serial not null,
    ref_id            int          not null,
    version           varchar(255),
    name              varchar(255) not null,
    app_id            int          not null,
    route             varchar(255) not null,
    page_content      text,
    is_body           smallint,
    parent_id         int          not null,
    "group"           varchar(255),
    depth             int,
    is_page           smallint not null,
    is_default        smallint not null,
    message           varchar(255),
    is_home           smallint not null default 0,
    content_blocks    text,
    tenant_id         varchar(60),
    renter_id         varchar(60),
    site_id           varchar(60),
    is_published      smallint not null,
    created_by        varchar(60)  not null,
    created_time      timestamp    not null default current_timestamp,
    last_updated_by   varchar(60)  not null,
    last_updated_time timestamp    not null default current_timestamp,
    primary key (id)
);

drop table if exists t_page_template;

create table t_page_template
(
    id                serial not null,
    name              varchar(255) not null,
    page_content      text,
    framework         varchar(255) not null,
    published         smallint,
    public            smallint,
    type              varchar(255) not null,
    status            varchar(255) not null,
    is_preset         smallint,
    image_url         text,
    description       varchar(2000),
    platform_id       int          not null,
    tenant_id         varchar(60),
    renter_id         varchar(60),
    site_id           varchar(60),
    created_by        varchar(60)  not null,
    created_time      timestamp    not null default current_timestamp,
    last_updated_by   varchar(60)  not null,
    last_updated_time timestamp    not null default current_timestamp,
    primary key (id)
);

drop table if exists t_component;

create table t_component
(
    id                 serial not null,
    version            varchar(255) not null,
    name               varchar(255) not null,
    name_en            varchar(255) not null,
    icon               varchar(255),
    description        varchar(2000),
    doc_url            varchar(255),
    screenshot         varchar(255),
    tags               varchar(255),
    keywords           varchar(255),
    dev_mode           varchar(255) not null,
    npm                text     not null,
    "group"            varchar(255),
    category           varchar(255),
    priority           int,
    snippets           text,
    schema_fragment    text,
    configure          text,
    public             int,
    framework          varchar(255) not null,
    is_official        smallint,
    is_default         smallint,
    tiny_reserved      smallint,
    component_metadata text,
    library_id         int,
    tenant_id          varchar(60),
    renter_id          varchar(60),
    site_id            varchar(60),
    created_by         varchar(60)  not null,
    created_time       timestamp    not null default current_timestamp,
    last_updated_by    varchar(60)  not null,
    last_updated_time  timestamp    not null default current_timestamp,
    primary key (id),
    constraint u_idx_component unique (tenant_id, name_en, version)
);

drop table if exists r_material_category;

create table r_material_category
(
    id          serial not null,
    material_id int not null,
    category_id int not null,
    primary key (id)
);

drop table if exists r_material_history_block;

create table r_material_history_block
(
    id                  serial not null,
    material_history_id int not null,
    block_history_id    int not null,
    primary key (id)
);

drop table if exists r_material_history_component;

create table r_material_history_component
(
    id                  serial not null,
    material_history_id int not null,
    component_id        int not null,
    primary key (id)
);

drop table if exists r_material_component;

create table r_material_component
(
    id           serial not null,
    material_id  int not null,
    component_id int not null,
    primary key (id)
);

drop table if exists r_material_block;

create table r_material_block
(
    id          serial not null,
    material_id int not null,
    block_id    int not null,
    primary key (id)
);

drop table if exists t_i18n_entry;

create table t_i18n_entry
(
    id                serial not null,
    key               varchar(255)  not null,
    content           varchar(3000) not null,
    host_id           int           not null,
    host_type         varchar(255)  not null,
    lang_id           int,
    tenant_id         varchar(60),
    renter_id         varchar(60),
    site_id           varchar(60),
    created_by        varchar(60)   not null,
    created_time      timestamp     not null default current_timestamp,
    last_updated_by   varchar(60)   not null,
    last_updated_time timestamp     not null default current_timestamp,
    primary key (id),
    constraint u_idx_i18n_entity unique (key, host_id, host_type,lang_id)
);

drop table if exists t_i18n_lang;

create table t_i18n_lang
(
    id                serial not null,
    lang              varchar(255) not null,
    label             varchar(255) not null,
    created_by        varchar(60)  not null,
    created_time      timestamp    not null default current_timestamp,
    last_updated_by   varchar(60)  not null,
    last_updated_time timestamp    not null default current_timestamp,
    primary key (id),
    constraint u_idx_18n_lang unique (lang)
);

drop table if exists t_datasource;

create table t_datasource
(
    id                serial not null,
    name              varchar(255) not null,
    data              text,
    tpl               int,
    app_id            int,
    platform_id       int,
    description       varchar(2000),
    tenant_id         varchar(60),
    renter_id         varchar(60),
    site_id           varchar(60),
    created_by        varchar(60)  not null,
    created_time      timestamp    not null default current_timestamp,
    last_updated_by   varchar(60)  not null,
    last_updated_time timestamp    not null default current_timestamp,
    primary key (id),
    constraint u_idx_datasource unique (tenant_id, platform_id, name)
);

drop table if exists t_task_record;

create table t_task_record
(
    id                serial not null,
    team_id           int,
    task_type         int,
    build_id          int,
    task_name         varchar(255),
    task_status       int,
    task_result       text,
    progress          varchar(255),
    ratio             int,
    progress_percent  int,
    indicator         text,
    tenant_id         varchar(60),
    renter_id         varchar(60),
    site_id           varchar(60),
    created_by        varchar(60) not null,
    created_time      timestamp   not null default current_timestamp,
    last_updated_by   varchar(60) not null,
    last_updated_time timestamp   not null default current_timestamp,
    primary key (id)
);

drop table if exists t_tenant;

create table t_tenant
(
    id                serial not null,
    org_code          varchar(255),
    name_cn           varchar(255) not null,
    name_en           varchar(255),
    description       varchar(2000),
    created_by        varchar(60)  not null,
    created_time      timestamp    not null default current_timestamp,
    last_updated_by   varchar(60)  not null,
    last_updated_time timestamp    not null default current_timestamp,
    primary key (id),
    constraint u_idx_tenant unique (name_cn)
);

drop table if exists t_user;

create table t_user
(
    id                serial not null,
    username          varchar(255) not null,
    email             varchar(255) not null,
    role              varchar(255),
    enable            smallint,
    is_admin          smallint,
    is_public         smallint,
    tenant_id         varchar(60),
    renter_id         varchar(60),
    site_id           varchar(60),
    created_by        varchar(60)  not null,
    created_time      timestamp    not null default current_timestamp,
    last_updated_by   varchar(60)  not null,
    last_updated_time timestamp    not null default current_timestamp,
    primary key (id),
    constraint u_idx_user unique (username)
);

drop table if exists t_block_carriers_relation;

create table t_block_carriers_relation
(
    id                serial not null,
    block_id          int         not null,
    host_id           int         not null,
    host_type         varchar(60),
    version           varchar(60) not null,
    tenant_id         varchar(60),
    renter_id         varchar(60),
    site_id           varchar(60),
    created_by        varchar(60) not null,
    created_time      timestamp   not null default current_timestamp,
    last_updated_by   varchar(60) not null,
    last_updated_time timestamp   not null default current_timestamp,
    primary key (id),
    constraint u_idx_block_carriers_relation unique (host_id, host_type, block_id)
);

drop table if exists r_block_group_block;

create table r_block_group_block
(
    id             serial not null,
    block_id       int not null,
    block_group_id int not null,
    primary key (id),
    constraint u_idx_block_group_block unique (block_id, block_group_id)
);

drop table if exists t_component_library;

create table t_component_library
(
    id                serial not null,
    version           varchar(255) not null,
    name              varchar(255) not null,
    app_id            int,
    package           varchar(255) not null,
    registry          varchar(255),
    framework         varchar(255) not null,
    description       varchar(2000),
    script            varchar(255),
    css               varchar(255),
    bundle            varchar(255),
    dependencies      text,
    others            text,
    thumbnail         varchar(255),
    public            int,
    is_started        smallint,
    is_official       smallint,
    is_default        smallint,
    tenant_id         varchar(60),
    renter_id         varchar(60),
    site_id           varchar(60),
    created_by        varchar(60)  not null,
    created_time      timestamp    not null default current_timestamp,
    last_updated_by   varchar(60)  not null,
    last_updated_time timestamp    not null default current_timestamp,
    primary key (id),
    constraint u_idx_component_library unique (tenant_id, name, version)
);
