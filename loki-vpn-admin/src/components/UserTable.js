import React, { useEffect, useState } from 'react';
import axios from 'axios';
import PageHeader from './PageHeader';

const UserTable = () => {
    const [users, setUsers] = useState([]);

    useEffect(() => {
        axios.get('/api/admin/appusers').then((response) => setUsers(response.data));
    }, []);

    return (
        <div className="card">
            <div className="card-header bg-primary text-white">
                <PageHeader title="Список пользователей" />
            </div>
            <div className="card-body">
                <table className="table table-striped">
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>Имя пользователя</th>
                        <th>Роль</th>
                        <th>Chat ID</th>
                    </tr>
                    </thead>
                    <tbody>
                    {users.map((user) => (
                        <tr key={user.id}>
                            <td>{user.id}</td>
                            <td>{user.username}</td>
                            <td>{user.role}</td>
                            <td>{user.chatId}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default UserTable;
