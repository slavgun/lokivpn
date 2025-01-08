import React, { useState, useEffect } from 'react';
import axios from 'axios';

const AdminManagement = () => {
    const [admins, setAdmins] = useState([]);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');

    // Загрузка списка администраторов
    useEffect(() => {
        fetchAdmins();
    }, []);

    const fetchAdmins = async () => {
        try {
            const response = await axios.get('/api/admins');
            setAdmins(response.data);
        } catch (error) {
            console.error('Ошибка при загрузке администраторов:', error);
        }
    };

    const handleAddAdmin = async () => {
        if (!username || !password) {
            alert('Введите имя пользователя и пароль');
            return;
        }
        try {
            await axios.post('/api/admins', { username, password });
            alert('Администратор добавлен');
            setUsername('');
            setPassword('');
            fetchAdmins(); // Обновляем список администраторов
        } catch (error) {
            console.error('Ошибка при добавлении администратора:', error);
            alert('Ошибка при добавлении администратора');
        }
    };

    const handleDeleteAdmin = async (adminUsername) => {
        if (!window.confirm(`Вы уверены, что хотите удалить администратора ${adminUsername}?`)) {
            return;
        }
        try {
            await axios.delete(`/api/admins/${adminUsername}`);
            alert('Администратор удалён');
            fetchAdmins(); // Обновляем список администраторов
        } catch (error) {
            console.error('Ошибка при удалении администратора:', error);
            alert('Ошибка при удалении администратора');
        }
    };

    return (
        <div className="container mt-5">
            <h2>Управление администраторами</h2>
            <div className="mb-4">
                <h4>Добавить администратора</h4>
                <div className="mb-3">
                    <label htmlFor="username" className="form-label">Имя пользователя:</label>
                    <input
                        type="text"
                        id="username"
                        className="form-control"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        placeholder="Введите имя пользователя"
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="password" className="form-label">Пароль:</label>
                    <input
                        type="password"
                        id="password"
                        className="form-control"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="Введите пароль"
                    />
                </div>
                <button className="btn btn-primary" onClick={handleAddAdmin}>
                    Добавить администратора
                </button>
            </div>

            <hr />

            <h4>Список администраторов</h4>
            <table className="table table-bordered">
                <thead>
                <tr>
                    <th>Имя пользователя</th>
                    <th>Действия</th>
                </tr>
                </thead>
                <tbody>
                {admins.map((admin) => (
                    <tr key={admin.id}>
                        <td>{admin.username}</td>
                        <td>
                            <button
                                className="btn btn-danger"
                                onClick={() => handleDeleteAdmin(admin.username)}
                            >
                                Удалить
                            </button>
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
};

export default AdminManagement;
