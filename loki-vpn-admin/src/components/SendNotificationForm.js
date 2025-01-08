import React, { useState } from 'react';
import axios from 'axios';
import PageHeader from './PageHeader';

const SendNotificationForm = () => {
    const [message, setMessage] = useState('');
    const [recipientType, setRecipientType] = useState('all'); // 'all' или 'user'
    const [username, setUsername] = useState('');

    const handleSend = async () => {
        if (!message.trim()) {
            alert('Введите сообщение');
            return;
        }

        try {
            if (recipientType === 'all') {
                await axios.post('/api/admin/notifyAllUsers', { message });
            } else if (recipientType === 'user' && username.trim()) {
                await axios.post(`/api/admin/notifyUser/${username}`, { message });
            } else {
                alert('Введите имя пользователя');
                return;
            }
            alert('Уведомление отправлено');
            setMessage('');
            setUsername('');
        } catch (error) {
            console.error(error);
            alert('Ошибка при отправке уведомления');
        }
    };

    return (
        <div className="container mt-5">
            <div className="card">
                <div className="card-header bg-primary text-white">
                    <PageHeader title="Отправить уведомление" />
                </div>
                <div className="card-body">
                    <div className="mb-3">
                        <label className="form-label">Выберите получателя:</label>
                        <div>
                            <div className="form-check form-check-inline">
                                <input
                                    type="radio"
                                    id="allUsers"
                                    name="recipient"
                                    value="all"
                                    className="form-check-input"
                                    checked={recipientType === 'all'}
                                    onChange={() => setRecipientType('all')}
                                />
                                <label htmlFor="allUsers" className="form-check-label">
                                    Всем пользователям
                                </label>
                            </div>
                            <div className="form-check form-check-inline">
                                <input
                                    type="radio"
                                    id="specificUser"
                                    name="recipient"
                                    value="user"
                                    className="form-check-input"
                                    checked={recipientType === 'user'}
                                    onChange={() => setRecipientType('user')}
                                />
                                <label htmlFor="specificUser" className="form-check-label">
                                    Пользователю
                                </label>
                            </div>
                        </div>
                    </div>
                    {recipientType === 'user' && (
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
                    )}
                    <div className="mb-3">
                        <label htmlFor="message" className="form-label">
                            Сообщение:
                        </label>
                        <textarea
                            id="message"
                            className="form-control"
                            value={message}
                            onChange={(e) => setMessage(e.target.value)}
                            rows="4"
                            placeholder="Введите текст сообщения"
                        ></textarea>
                    </div>
                    <button className="btn btn-primary" onClick={handleSend}>
                        Отправить
                    </button>
                </div>
            </div>
        </div>
    );
};

export default SendNotificationForm;
