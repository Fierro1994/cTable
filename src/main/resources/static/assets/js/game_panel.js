document.addEventListener('DOMContentLoaded', function() {
  var profileLink = document.getElementById('profile-link');
  var profilePopup = document.getElementById('profile-popup');
  var closePopup = document.querySelector('.close');

  profileLink.addEventListener('click', function(event) {
    event.preventDefault();
    profilePopup.style.display = 'block';
  });

  closePopup.addEventListener('click', function() {
    profilePopup.style.display = 'none';
  });

  window.addEventListener('click', function(event) {
    if (event.target == profilePopup) {
      profilePopup.style.display = 'none';
    }
  });
});