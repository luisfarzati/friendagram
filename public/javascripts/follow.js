window.DELETED_PHOTO = null;

angular.module('instagram-tool', []).config(['$routeProvider', function($routeProvider) {
	$routeProvider.when('/:username/jobs/follow/tasks', { templateUrl: 'public/partials/follow-tasks.html', controller: FollowTasksController });
	$routeProvider.when('/home', { templateUrl: 'public/partials/newsfeed.html', controller: NewsfeedController });
}]);

function NewsfeedController($scope, $http) {
	clearIntervals();
}

function AccountsController($scope, $http) {
	clearIntervals();
	$http.get(ROUTES.accounts()).success(function(accounts) {
		$scope.accounts = accounts;
	});
}

function FollowTasksController($scope, $routeParams, $http) {
	clearIntervals();
	intervals.push(setInterval(function() {
		$http.get(ROUTES.tasks({username: $routeParams.username, job: 'follow'}))
		 .success(function(tasks) {
			$scope.tasks = tasks;
			console.log($routeParams.username + ':follow:tasks = ' + tasks.length);
			if(window.DELETED_PHOTO != null) {
				$('#tasksAlert').removeClass('alert-info alert-error').addClass('alert-success').html('<button class="close" data-dismiss="alert">×</button>The <a href="http://instagram.com/p/'+window.DELETED_PHOTO+'" target="_blank">selected photo</a> has been removed from the queue.').show();
				window.DELETED_PHOTO = null;
			}
		 });
	}, 2000));

	$scope.username = window.CURRENT_USERNAME = $routeParams.username;
}

intervals = [];

function clearIntervals() {
	$.each(intervals, function(i,v) { window.clearInterval(v); });
}

var abortEnqueue = false;
var inProcess = false;

$('#addPhotoModal').on('hide', function() { 
	abortEnqueue = inProcess; 
	console.log('aborting=' + abortEnqueue); 
});

$('#addPhotoModal').on('show', function() { 
	$('#addPhotoSubmit').removeAttr('disabled');
	$('#addPhotoCancel').text('Close').removeAttr('disabled').removeClass('btn-danger'); 
	$('#shortCode').val('');
	$('#addPhotoProgress').hide();
	$('#addPhotoCode').show();
	$('#addPhotoAlert').hide();
});

function deletePhoto(shortCode) {
	$('#tasksAlert').removeClass('alert-info alert-success').addClass('alert-info').html('Removing photo from the queue...').show();
	$.ajax({
		url: ROUTES.delete({
			username: window.CURRENT_USERNAME, 
			job: 'follow', 
			shortcode: shortCode 
		}),
		type: 'DELETE',
		error: function(e,s) {
			$('#tasksAlert').removeClass('alert-info alert-success').addClass('alert-error').html(s).show(); 
		},
		success: function() {
			$('#'+shortCode).addClass('deleted');
			window.DELETED_PHOTO = shortCode;
		}
	});
}

function addPhoto() {
	lookupPhoto($('#shortCode').val().trim().replace('/',''));
}

function lookupPhoto(shortCode) {
	$('#addPhotoCancel').text('Cancel').addClass('btn-danger');
	if(shortCode == '') {
		$('#addPhotoAlert').removeClass('alert-success alert-info').addClass('alert-error ').text('Please insert a valid Instagram code.').show();
		$('#shortCode').focus();
		return;
	}
	$('#shortCode').focus();

	inProcess = true;
	
	console.log('lookupPhoto('+shortCode+')');

	$('#addPhotoSubmit').attr('disabled', 'disabled');
	$('#addPhotoAlert').removeClass('alert-error alert-success').addClass('alert-info').text('Looking up photo...').show();
	$('#addPhotoCode').hide();
	$('#addPhotoProgress').removeClass('progress-danger progress-success').addClass('progress-info')
	$('#addPhotoProgress .bar').css('width', '0%');
	$('#addPhotoProgress').show();
	setTimeout(function() {
		$('#addPhotoProgress .bar').css('width', '25%');
	}, 100);
	$('#preview')
		.load(function() {
			setTimeout(function() {
				retrievePhoto(shortCode);
			}, 500);
		})
		.error(function(e,x,r) {
			$('#addPhotoAlert').removeClass('alert-info alert-success').addClass('alert-error').text('Invalid photo!'); 
			$('#addPhotoProgress').removeClass('progress-info progress-success').addClass('progress-danger');
			setTimeout(function() {
				$('#addPhotoSubmit').removeAttr('disabled');
				$('#addPhotoCancel').text('Close').removeAttr('disabled').removeClass('btn-danger'); 
				$('#addPhotoProgress').hide();
				$('#addPhotoCode').show();
				inProcess = abortEnqueue = false;
			}, 500);
		})
		.attr('src','http://instagram.com/p/' + shortCode + '/media?size=t');
}

function retrievePhoto(shortCode) {
	if(abortEnqueue) { 
		inProcess = abortEnqueue = false;
		console.log('retrievePhoto('+shortCode+') aborted');
		return; 
	}
	console.log('retrievePhoto('+shortCode+')');
	
	$('#addPhotoProgress .bar').css('width', '66%');
	$('#addPhotoAlert').removeClass('alert-error alert-success').addClass('alert-info').text('Retrieving photo information...');
	$.ajax({
		url: 'http://api.instagram.com/oembed?url=http://instagr.am/p/' + shortCode,
		dataType: 'jsonp',
		type: 'GET',
		success: function(data) {
			setTimeout(function() {
				enqueuePhoto(shortCode);
			}, 500);
		}
	});
}

function enqueuePhoto(shortCode) {
	if(abortEnqueue) { 
		inProcess = abortEnqueue = false;
		console.log('enqueuePhoto('+shortCode+') aborted');
		return; 
	}
	console.log('enqueuePhoto('+shortCode+')');

	$('#addPhotoAlert').removeClass('alert-error alert-success').addClass('alert-info').text('Adding photo to the queue...');
	$.ajax({
		url: ROUTES.enqueue({
			username: window.CURRENT_USERNAME, 
			job: 'follow', 
			shortcode: shortCode 
		}),
		type: 'PUT',
		error: function(e,s) {
			$('#addPhotoAlert').removeClass('alert-info alert-success').addClass('alert-error').text(s); 
			$('#addPhotoProgress').removeClass('progress-info progress-success').addClass('progress-danger');
			setTimeout(function() {
				$('#addPhotoSubmit').removeAttr('disabled');
				$('#addPhotoCancel').text('Close').removeAttr('disabled').removeClass('btn-danger'); 
				$('#addPhotoProgress').hide();
				$('#addPhotoCode').show();
				inProcess = abortEnqueue = false;
			}, 1000);
		},
		success: function() {
			$('#addPhotoAlert').removeClass('alert-info alert-error').addClass('alert-success').text('Done!');
			$('#addPhotoProgress').removeClass('progress-info progress-danger').addClass('progress-success');
			$('#addPhotoProgress .bar').css('width', '100%');

			inProcess = abortEnqueue = false;
			setTimeout(function() {
				$('#addPhotoModal').modal('hide');
			}, 1000);
		}
	});
}