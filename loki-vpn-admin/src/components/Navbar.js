import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

const Navbar = () => {
    const [isDarkTheme, setIsDarkTheme] = useState(false);

    const handleThemeToggle = () => {
        const newTheme = !isDarkTheme;
        setIsDarkTheme(newTheme);
        document.body.classList.toggle('dark-theme', newTheme);
        localStorage.setItem('theme', newTheme ? 'dark' : 'light');
    };

    useEffect(() => {
        const savedTheme = localStorage.getItem('theme') === 'dark';
        setIsDarkTheme(savedTheme);
        if (savedTheme) {
            document.body.classList.add('dark-theme');
        }
    }, []);

    return (
        <nav className="navbar navbar-expand-lg bg-primary text-white">
            <div className="container-fluid d-flex justify-content-between align-items-center">
                <Link to="/" className="navbar-brand text-white fw-bold">
                    Админка LOKI VPN
                </Link>
                <button
                    className="btn btn-outline-light"
                    onClick={handleThemeToggle}
                >
                    {isDarkTheme ? 'Светлая тема' : 'Тёмная тема'}
                </button>
            </div>
        </nav>
    );
};

export default Navbar;
