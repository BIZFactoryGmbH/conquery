// Bootstrap enable tooltips everywhere
$(function () { $('[data-toggle="tooltip"]').tooltip() })

const ToastTypes = {
	INFO: "badge-info",
	SUCCESS: "badge-success",
	WARNING: "badge-warning",
	ERROR: "badge-danger"
};

async function rest(url, options) {
	var res = await fetch(
		url,
		{
			method: 'get',
			credentials: 'same-origin',
			headers: {
				'Content-Type': 'application/json'
			},
			...options
		}
	);
	showMessageForResponse(res);
	return res;
}

function getToast(type, title, text, smalltext = "") {
	if (!type) type = ToastTypes.INFO;

	let toast = document.createElement("div");
	toast.classList.add("toast");
	toast.setAttribute("role", "alert");
	toast.setAttribute("aria-live", "assertive");
	toast.setAttribute("aria-atomic", "true");
	toast.setAttribute("style", "width: 500px; max-width: none");
	toast.innerHTML = `
            				<div class="toast-header">
            					<strong class="mr-auto badge badge-pill ${type}" style="font-size: 1rem;">${title}</strong>
            					${smalltext ? '<small class="text-muted">' + smalltext + '</small>' : ''}
            					<button type="button" class="ml-2 mb-1 close" data-dismiss="toast" aria-label="Close">
            						<span aria-hidden="true">&times;</span>
            					</button>
            				</div>
            				<div class="toast-body" style="white-space: normal; overflow-x: auto;">
            					${text.trim()}
            				</div>
            				`;
	return toast;
}



function showToastMessage(type, title, text, smalltext = "") {
	let toastContainer = document.getElementById("toast-container");
	let toast = getToast(type, title, text, smalltext);
	toastContainer.appendChild(toast);
	$(toast).toast({ delay: 10000 });
	$(toast).toast('show');
}

async function showMessageForResponse(response) {
	if (response) {
		try {
			let body = await response.json();
			if (!response.ok) {
				showToastMessage(ToastTypes.ERROR, "Error", "The send request came back with the following error: " + JSON.stringify(body), "Status " + response.status);
			}
		} catch (e) {
			// ignore, because some responses don't have a body
		}
	}
}

function logout() {
	event.preventDefault();
	rest('/${ctx.staticUriElem.ADMIN_SERVLET_PATH}/logout')
		.then(function () { location.reload() });
}

function postFile(event, url) {
	event.preventDefault();

	let inputs = event.target.getElementsByClassName("restparam");
	if (inputs.length != 1) {
		console.log('Unexpected number of inputs in ' + inputs);
	}

	var file;

	for (var i = 0; i < inputs[0].files.length; i++) {
		let file = inputs[0].files[i];
		let reader = new FileReader();
		reader.onload = function () {
			let json = reader.result;
			fetch(url, {
				method: 'post', credentials: 'same-origin', body: json, headers: {
					"Content-Type": "application/json"
				}
			})
				.then(function (response) {
					if (response.ok) {
						setTimeout(() => location.reload(), 2000);
						showToastMessage(ToastTypes.SUCCESS, "Success", "The file has been posted successfully");
					} else {
						showMessageForResponse(response);
					}
				})
				.catch(function (error) {
					showToastMessage(ToastTypes.ERROR, "Error", "There has been a problem with posting a file: " + error.message);
					console.log('There has been a problem with posting a file', error.message);
				});
		};
		reader.readAsText(file);
	}
}


// Get to recent tab: https://stackoverflow.com/a/19015027
$('#myTab a').click(function (e) {
	e.preventDefault();
	$(this).tab('show');
});

// store the currently selected tab in the hash value
$("ul.nav-tabs > li > a").on("shown.bs.tab", function (e) {
	var id = $(e.target).attr("href").substr(1);
	window.location.hash = id;
});

// on load of the page: switch to the currently selected tab
var hash = window.location.hash;
$('#myTab a[href="' + hash + '"]').tab('show');