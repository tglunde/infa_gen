{% extends "create_table.sql" %}

{% block tablename %}{{source_name}}_{{table_name}}_HA{% endblock %}

{% block technicalfield %}
    {{table_name}}_SK BIGINT NOT NULL,
    {{table_name}}_LDTS TIMESTAMP NOT NULL,
    {{table_name}}_RSRC VARCHAR(20) NOT NULL,
{% endblock %}
{% block tablegrant %}
GRANT INSERT,DELETE,ALTER ON {{source_name}}_{{table_name}}_HA TO ETLRLOAD;
{% endblock %}