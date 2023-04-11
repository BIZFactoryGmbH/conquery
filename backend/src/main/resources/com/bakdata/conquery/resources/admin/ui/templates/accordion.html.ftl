<#macro accordionGroup class="" style="">
  <div class="accordion my-2 ${class}" style="${style}">
    <#nested />
  </div>
</#macro>
<#macro accordion summary infoText="" class="" style="">
  <div class="card accordion-card ${class}" style="${style}" data-test-id="accordion-${summary}">
    <div
      class="card-header d-inline-flex justify-content-between"
      style="user-select: none; cursor: pointer;"
      onclick="if (!$(this).next().hasClass('show')) {history.replaceState(null, '', '#${summary}')} $(this).next().collapse('toggle');"
    >
      <div>
        <h5 class="p-0 m-0">${summary}</h5>
      </div>
      <div class="row pr-3">
        <div class="text-secondary">${infoText}</div>
      </div>
    </div>
    <div id="collapse-${summary}" class="collapse">
      <div class="card-body py-0">
        <#nested />
      </div>
    </div>
    <script>
      if (location.hash.replace('#', '') == '${summary}') {
        $('#collapse-${summary}').addClass('show');
      }
    </script>
  </div>
</#macro>