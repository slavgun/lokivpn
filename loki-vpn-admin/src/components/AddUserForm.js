import React, { useState } from 'react';
import axios from 'axios';
import PageHeader from './PageHeader'; // Импорт компонента

const AddUserForm = () => {
    const [username, setUsername] = useState('');
    const [role, setRole] = useState('');
    const [chatId, setChatId] = useState('');

    const handleAddUser = async () => {
        if (!username.trim() || !chatId.trim()) {
            alert('Введите имя пользователя и Chat ID');
            return;
        }

        try {
            await axios.post('/api/admin/appusers', { username, role, chatId });
            alert('Пользователь добавлен');
            setUsername('');
            setRole('');
            setChatId('');
        } catch (error) {
            console.error(error);
            alert('Ошибка при добавлении пользователя');
        }
    };

    return (
        <div className="container mt-5"> {/* Используем mt-5 для единых отступов */}
            <div className="card">
                <div className="card-header bg-primary text-white">
                    <PageHeader title="Добавить пользователя" /> {/* Внутри card-header */}
                </div>
                <div className="card-body">
                    <div className="mb-3">
                        <label htmlFor="username" className="form-label">
                            Имя пользователя:
                        </label>
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
                        <label htmlFor="role" className="form-label">
                            Роль:
                        </label>
                        <input
                            type="text"
                            id="role"
                            className="form-control"
                            value={role}
                            onChange={(e) => setRole(e.target.value)}
                            placeholder="ADMIN, USER и т.д."
                        />
                    </div>
                    <div className="mb-3">
                        <label htmlFor="chatId" className="form-label">
                            Chat ID:
                        </label>
                        <input
                            type="text"
                            id="chatId"
                            className="form-control"
                            value={chatId}
                            onChange={(e) => setChatId(e.target.value)}
                            placeholder="Введите Chat ID"
                        />
                    </div>
                    <button className="btn btn-primary" onClick={handleAddUser}>
                        Добавить
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AddUserForm;
