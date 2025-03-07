%define __jar_repack %{nil}
%define user ignite
%define _libdir /usr/lib
%define _log %{_var}/log
%define _sharedstatedir /var/lib
%define _binaries_in_noarch_packages_terminate_build 0


#-------------------------------------------------------------------------------
#
# Packages' descriptions
#

Name:             apache-ignite
Version:          2.15.0
Release:          1
Summary:          Apache Ignite In-Memory Computing, Database and Caching Platform
Group:            Development/System
License:          /usr/ndp/3.0/%{name}/LICENSE
URL:              https://ignite.apache.org/
Source:           %{name}-%{version}-bin.zip
Requires:         chkconfig
Requires(pre):    shadow-utils
Provides:         %{name}
AutoReq:          no
AutoProv:         no
BuildArch:        noarch
%description
Ignite™ is a memory-centric distributed database, caching, and processing
platform for transactional, analytical, and streaming workloads, delivering
in-memory speeds at petabyte scale


%prep
#-------------------------------------------------------------------------------
#
# Prepare step: unpack sources
#

%setup -q -n %{name}-%{version}-bin


#%pre
#-------------------------------------------------------------------------------
#
# Preinstall scripts
# $1 can be:
#     1 - Initial install 
#     2 - Upgrade
#


%post
#-------------------------------------------------------------------------------
#
# Postinstall scripts
# $1 can be:
#     1 - Initial installation
#     2 - Upgrade
#

echoUpgradeMessage () {
    echo "======================================================================================================="
    echo "  WARNING: Updating Apache Ignite's cluster version requires updating every node before starting grid  "
    echo "======================================================================================================="
}

setPermissions () {
    chown -R %{user}:%{user} %{_sharedstatedir}/%{name} /var/log/%{name}
}

case $1 in
    1|configure)
        # DEB postinst upgrade
        if [ ! -z "${2}" ]; then
            echoUpgradeMessage
        fi

        # Add user for service operation
        useradd -r -d /home/%{user} -s /bin/bash %{user}

        # Change ownership for work and log directories
        setPermissions

        # Install alternatives
        # Commented out until ignitesqlline is ready to work from any user
        #update-alternatives --install /usr/bin/ignitesqlline ignitesqlline %{_datadir}/%{name}/bin/sqlline.sh 0
        #update-alternatives --auto ignitesqlline
        #update-alternatives --display ignitesqlline
        ;;
    2)
        # RPM postinst upgrade
        echoUpgradeMessage

        # Workaround for upgrade from 2.4.0
        if [ -d /usr/com/apache-ignite/ ]; then
            for file in /usr/com/apache-ignite/*; do
                if [ ! -h $file ]; then
                    cp -rf $file %{_sharedstatedir}/%{name}/
                fi
            done
        fi

        # Change ownership for work and log directories (yum resets permissions on upgrade nevertheless)
        setPermissions
        ;;
esac


%preun
#-------------------------------------------------------------------------------
#
# Pre-uninstall scripts
# $1 can be:
#     0 - Uninstallation
#     1 - Upgrade
#

stopIgniteNodes () {
    if ! $(grep -q "Microsoft" /proc/version); then
        systemctl stop 'apache-ignite@*'
    fi
    ps ax | grep '\-DIGNITE_HOME' | head -n-1 | awk {'print $1'} | while read pid; do
        kill -INT ${pid}
    done
}

case $1 in
    0|remove)
        # Stop all nodes (both service and standalone)
        stopIgniteNodes

        # Remove alternatives
        # Commented out until ignitesqlline is ready to work from any user
        #update-alternatives --remove ignitesqlline /usr/share/%{name}/bin/sqlline.sh
        #update-alternatives --display ignitesqlline || true
        ;;
    1|upgrade)
        # Stop all nodes (both service and standalone)
        echo "=================================================================================="
        echo "  WARNING: All running Apache Ignite's nodes will be stopped upon package update  "
        echo "=================================================================================="
        stopIgniteNodes
        ;;
esac


%postun
#-------------------------------------------------------------------------------
#
# Post-uninstall scripts
# $1 can be:
#     0 - Uninstallation
#     1 - Upgrade
#

case $1 in
    0|remove)
        # Remove user
        userdel %{user}

        # Remove service PID directory
        rm -rfv /var/run/%{name}

        # Remove firewalld rules if firewalld is installed and running
        if [[ "$(type firewall-cmd &>/dev/null; echo $?)" -eq 0 && "$(systemctl is-active firewalld)" == "active" ]]
        then
            for port in s d
            do
                firewall-cmd --permanent --direct --remove-rule ipv4 filter INPUT 0 -p tcp -m multiport --${port}ports 11211:11220,47500:47509,47100:47109 -j ACCEPT &>/dev/null
                firewall-cmd --permanent --direct --remove-rule ipv4 filter INPUT 0 -p udp -m multiport --${port}ports 47400:47409 -j ACCEPT &>/dev/null
            done
            firewall-cmd --permanent --direct --remove-rule ipv4 filter INPUT 0 -m pkttype --pkt-type multicast -j ACCEPT &>/dev/null
            systemctl restart firewalld
        fi
        ;;
    1|upgrade)
        :
        ;;
esac


%install
#-------------------------------------------------------------------------------
#
# Prepare packages' layout
#

# Create base directory structure
mkdir -p %{buildroot}/usr/ndp/3.0/%{name}
mkdir -p %{buildroot}/var/log/%{name}
mkdir -p %{buildroot}%{_sharedstatedir}/%{name}
mkdir -p %{buildroot}/etc/systemd/system
mkdir -p %{buildroot}/usr/bin

# Copy nessessary files and remove *.bat files
cp -rf * %{buildroot}/usr/ndp/3.0/%{name}
find %{buildroot}/usr/ndp/3.0/%{name} -name *.bat -exec rm -rf {} \;

# Setup systemctl service
cp -f %{_sourcedir}/name.service %{buildroot}/etc/systemd/system/%{name}@.service
cp -rf %{_sourcedir}/service.sh %{buildroot}/usr/ndp/3.0/%{name}/bin/
chmod +x %{buildroot}/usr/ndp/3.0/%{name}/bin/service.sh
for file in %{buildroot}/etc/systemd/system/%{name}@.service %{buildroot}/usr/ndp/3.0/%{name}/bin/service.sh
do
    sed -i -r -e "s|#name#|%{name}|g" \
              -e "s|#user#|%{user}|g" \
        ${file}
    mv -f ${file}-r /tmp/a.txt
done


# Map work and log directories
ln -sf %{_sharedstatedir}/%{name} %{buildroot}/usr/ndp/3.0/%{name}/work
ln -sf /var/log/%{name} %{buildroot}%{_sharedstatedir}/%{name}/log


%files
#-------------------------------------------------------------------------------
#
# Package file list check
#

%dir /usr/ndp/3.0/%{name}
%dir %{_sharedstatedir}/%{name}
%dir /var/log/%{name}

/usr/ndp/3.0/%{name}/benchmarks
/usr/ndp/3.0/%{name}/bin
/usr/ndp/3.0/%{name}/config
/usr/ndp/3.0/%{name}/libs
/usr/ndp/3.0/%{name}/platforms
/usr/ndp/3.0/%{name}/examples
/usr/ndp/3.0/%{name}/work
/usr/ndp/3.0/%{name}/docs
/etc/systemd/system/%{name}@.service
%{_sharedstatedir}/%{name}/log

%doc /usr/ndp/3.0/%{name}/README.txt
%doc /usr/ndp/3.0/%{name}/NOTICE
%doc /usr/ndp/3.0/%{name}/RELEASE_NOTES.txt
%doc /usr/ndp/3.0/%{name}/MIGRATION_GUIDE.txt
%doc /usr/ndp/3.0/%{name}/LICENSE


%changelog
#-------------------------------------------------------------------------------
#
# Changelog
#

* Fri Sep 09 2022 Taras Ledkov <tledkov@apache.org> - 2.13.0-1
- Updated Apache Ignite to version 2.14.0

* Thu Apr 07 2022 Nikita Amelchev <namelchev@apache.org> - 2.13.0-1
- Updated Apache Ignite to version 2.13.0

* Thu Oct 21 2021 Nikita Amelchev <namelchev@apache.org> - 2.12.0-1
- Updated Apache Ignite to version 2.12.0

* Fri Jul 09 2021 Alexey Gidaspov <olive.crow@gmail.com> - 2.11.0-1
- Updated Apache Ignite to version 2.11.0

* Mon Feb 01 2021 Maxim Muzafarov <mmuzaf@apache.org> - 2.10.0-1
- Updated Apache Ignite to version 2.10.0

* Wed Nov 25 2020 Yaroslav Molochkov <y.n.molochkov@gmail.com> - 2.9.1-1
- Updated Apache Ignite to version 2.9.1

* Mon Sep 28 2020 Alexey Plekhanov <alexpl@apache.org> - 2.9.0-1
- Updated Apache Ignite to version 2.9.0

* Wed May 20 2020 Nikolay Izhikov <nizhikov@apache.org> - 2.8.1-1
- Updated Apache Ignite to version 2.8.1

* Thu Feb 20 2020 Maxim Muzafarov <mmuzaf@apache.org> - 2.8.0-1
- Updated Apache Ignite to version 2.8.0

* Tue Sep 10 2019 Peter Ivanov <mr.weider@gmail.com> - 2.7.6-1
- Updated Apache Ignite to version 2.7.6

* Fri Apr 12 2019 Peter Ivanov <mr.weider@gmail.com> - 2.7.5-1
- Updated Apache Ignite to version 2.7.5

* Thu Jul 26 2018 Peter Ivanov <mr.weider@gmail.com> - 2.7.0-1
- Updated Apache Ignite to version 2.7.0

* Fri Jun 15 2018 Peter Ivanov <mr.weider@gmail.com> - 2.6.0-1
- Updated Apache Ignite to version 2.6.0

* Tue Apr 17 2018 Peter Ivanov <mr.weider@gmail.com> - 2.5.0-1
- Updated Apache Ignite to version 2.5.0

* Wed Jan 17 2018 Peter Ivanov <mr.weider@gmail.com> - 2.4.0-1
- Initial package release
