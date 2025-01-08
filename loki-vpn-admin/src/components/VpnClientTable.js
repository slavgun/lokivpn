import React, { useEffect, useState } from 'react';
import axios from 'axios';
import PageHeader from './PageHeader';

const VpnClientTable = () => {
    const [vpnClients, setVpnClients] = useState([]);

    useEffect(() => {
        axios.get('/api/admin/vpnclients').then((response) => setVpnClients(response.data));
    }, []);

    return (
        <div className="card">
            <div className="card-header bg-primary text-white">
                <PageHeader title="Список VPN-клиентов" />
            </div>
            <div className="card-body">
                <table className="table table-striped">
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>Имя клиента</th>
                        <th>Имя сервера</th>
                        <th>Тип устройства</th>
                        <th>Статус</th>
                    </tr>
                    </thead>
                    <tbody>
                    {vpnClients.map((client) => (
                        <tr key={client.id}>
                            <td>{client.id}</td>
                            <td>{client.clientName}</td>
                            <td>{client.serverName}</td>
                            <td>{client.deviceType}</td>
                            <td>{client.isAssigned ? 'Назначен' : 'Свободен'}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default VpnClientTable;
