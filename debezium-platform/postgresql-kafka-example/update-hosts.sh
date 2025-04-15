IP=$(kubectl cluster-info | sed -n 's/.*https:\/\/\([0-9.]*\).*/\1/p' | head -n 1)
HOSTNAME=${DEBEZIUM_PLATFORM_DOMAIN:-platform.debezium.io}
EXISTING=$(grep "$HOSTNAME" /etc/hosts)

if [ -z "$EXISTING" ]; then
    # Hostname not in hosts file, add it
    echo "$IP $HOSTNAME" | sudo tee -a /etc/hosts
    echo "Added new entry: $IP $HOSTNAME"
else
    # Hostname exists, check the associated IP
    EXISTING_IP=$(echo "$EXISTING" | awk '{print $1}')

    if [ "$EXISTING_IP" != "$IP" ]; then
        echo "WARNING: $HOSTNAME is already associated with IP $EXISTING_IP"
        echo "Current kubectl IP is $IP"
        read -p "Do you want to update the entry? (y/n) " -r
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            # Remove existing entry and add new one
            sudo sed -i "/$HOSTNAME/d" /etc/hosts
            echo "$IP $HOSTNAME" | sudo tee -a /etc/hosts
            echo "Updated hosts file with new IP"
        else
            echo "Hosts file not modified"
        fi
    else
        echo "Entry for $HOSTNAME already exists with the same IP"
    fi
fi