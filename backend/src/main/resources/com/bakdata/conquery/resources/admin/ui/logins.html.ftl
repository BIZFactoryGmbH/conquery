<!doctype html>
<html lang="en">
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

<title>Logins</title>
    <ol>
        <#list c as login_schema>
        <li><a href="${login_schema}">${login_schema}</a></li>
        </#list>
    </ol>
</html>