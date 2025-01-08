import React from 'react';
import { Link } from 'react-router-dom';

const Dashboard = () => {
    return (
        <div className="container mt-4">
            <div className="row g-4">
                {/* Пользователи */}
                <div className="col-md-4">
                    <div className="card h-100">
                        <div className="card-header">Пользователи</div>
                        <div className="card-body d-flex flex-column justify-content-between">
                            <p>Управление списком пользователей</p>
                            <Link to="/users" className="btn btn-primary mt-auto">
                                Перейти
                            </Link>
                        </div>
                    </div>
                </div>

                {/* VPN-клиенты */}
                <div className="col-md-4">
                    <div className="card h-100">
                        <div className="card-header">VPN-клиенты</div>
                        <div className="card-body d-flex flex-column justify-content-between">
                            <p>Просмотр и управление VPN-клиентами</p>
                            <Link to="/vpnclients" className="btn btn-primary mt-auto">
                                Перейти
                            </Link>
                        </div>
                    </div>
                </div>

                {/* Уведомления */}
                <div className="col-md-4">
                    <div className="card h-100">
                        <div className="card-header">Уведомления</div>
                        <div className="card-body d-flex flex-column justify-content-between">
                            <p>Отправить уведомление пользователям</p>
                            <Link to="/send-notification" className="btn btn-primary mt-auto">
                                Перейти
                            </Link>
                        </div>
                    </div>
                </div>

                {/* Добавить пользователя */}
                <div className="col-md-4">
                    <div className="card h-100">
                        <div className="card-header">Добавить пользователя</div>
                        <div className="card-body d-flex flex-column justify-content-between">
                            <p>Новый пользователь</p>
                            <Link to="/add-user" className="btn btn-primary mt-auto">
                                Добавить
                            </Link>
                        </div>
                    </div>
                </div>

                {/* Добавить сервер */}
                <div className="col-md-4">
                    <div className="card h-100">
                        <div className="card-header">Добавить сервер</div>
                        <div className="card-body d-flex flex-column justify-content-between">
                            <p>Добавьте новый сервер</p>
                            <Link to="/add-server" className="btn btn-primary mt-auto">
                                Перейти
                            </Link>
                        </div>
                    </div>
                </div>

                {/* Управление администраторами */}
                <div className="col-md-4">
                    <div className="card h-100">
                        <div className="card-header">Администраторы</div>
                        <div className="card-body d-flex flex-column justify-content-between">
                            <p>Управление администраторами</p>
                            <Link to="/admins" className="btn btn-primary mt-auto">
                                Перейти
                            </Link>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;
