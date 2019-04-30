{% extends "create_table.sql" %}
{% block tablename %}{{source_name}}_{{table_name}}_R{% endblock %}
{% block tablegrant -%}
GRANT INSERT,DELETE,ALTER ON {{source_name}}_{{table_name}}_R TO ETLRLOAD;
{% endblock -%}