from django.conf.urls import patterns, include, url

# Uncomment the next two lines to enable the admin:
# from django.contrib import admin
# admin.autodiscover()

urlpatterns = patterns('',
    # Examples:
    # url(r'^$', 'django_tw.views.home', name='home'),
    # url(r'^django_tw/', include('django_tw.foo.urls')),
    url(r'^cgi-bin/get_markers.py$', 'web.views.get_markers'),
    url(r'^cgi-bin/register.py$', 'web.views.register'),
    url(r'^cgi-bin/updatelocation.py$', 'web.views.updatelocation'),
    url(r'^cgi-bin/user_submit.py$', 'web.views.user_submit'),

    # Uncomment the admin/doc line below to enable admin documentation:
    # url(r'^admin/doc/', include('django.contrib.admindocs.urls')),

    # Uncomment the next line to enable the admin:
    # url(r'^admin/', include(admin.site.urls)),
)
