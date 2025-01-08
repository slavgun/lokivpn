import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import UserTable from './components/UserTable';
import VpnClientTable from './components/VpnClientTable';
import SendNotificationForm from './components/SendNotificationForm';
import Dashboard from './components/Dashboard'; // Новая страница
import AddUserForm from './components/AddUserForm';
import AddServerForm from './components/AddServerForm';
import AdminManagement from './components/AdminManagement';

const App = () => {
    return (
        <Router>
            <Navbar />
            <div className="container mt-4">
                <Routes>
                    <Route path="/" element={<Dashboard />} />
                    <Route path="/users" element={<UserTable />} />
                    <Route path="/vpnclients" element={<VpnClientTable />} />
                    <Route path="/send-notification" element={<SendNotificationForm />} />
                    <Route path="/add-user" element={<AddUserForm />} />
                    <Route path="/add-server" element={<AddServerForm />} />
                    <Route path="/admins" element={<AdminManagement />} />
                </Routes>
            </div>
        </Router>
    );
};

export default App;