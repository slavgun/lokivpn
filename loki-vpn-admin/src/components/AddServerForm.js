import React, { useState } from 'react';
import axios from 'axios';
import PageHeader from './PageHeader';

const AddServerForm = () => {
    const [name, setName] = useState('');
    const [ipAddress, setIpAddress] = useState('');

    const handleAddServer = async () => {
        if (!name.trim() || !ipAddress.trim()) {
            alert('Введите имя и IP адрес');
            return;
        }

        try {
            await axios.post('/api/admin/servers', { name, ipAddress });
            alert('Сервер добавлен');
            setName('');
            setIpAddress('');
        } catch (error) {
            console.error(error);
            alert('Ошибка при добавлении сервера');
        }
    };

    return (
        <div className="card">
            <div className="card-header bg-primary text-white">
                <PageHeader title="Добавить сервер" />
            </div>
            <div className="card-body">
                <div className="mb-3">
                    <label htmlFor="name" className="form-label">
                        Имя сервера:
                    </label>
                    <input
                        type="text"
                        id="name"
                        className="form-control"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        placeholder="Введите имя сервера"
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="ipAddress" className="form-label">
                        IP адрес:
                    </label>
                    <input
                        type="text"
                        id="ipAddress"
                        className="form-control"
                        value={ipAddress}
                        onChange={(e) => setIpAddress(e.target.value)}
                        placeholder="Введите IP адрес"
                    />
                </div>
                <button className="btn btn-primary" onClick={handleAddServer}>
                    Добавить
                </button>
            </div>
        </div>
    );
};

export default AddServerForm;
