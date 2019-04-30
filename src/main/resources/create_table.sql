{% import "macros" %}
CREATE TABLE {% block tablename %}{{table_name}}{% endblock %} (
{% block technicalfield -%}{%- endblock -%}{%- block businessfield -%}{{ columnlist(columns) }}{%- endblock %}
)
{% block organizedby -%}{%- endblock -%}
;
{% block tablegrant -%}{% endblock -%}
{% block constraint -%}{% endblock -%}
