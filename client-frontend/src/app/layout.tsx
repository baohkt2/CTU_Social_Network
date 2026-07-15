import type {Metadata} from "next";
import {Geist, Geist_Mono} from "next/font/google";
import "./globals.css";
import {AuthProvider} from "@/contexts/AuthContext";
import QueryProvider from "@/contexts/QueryProvider";
import {ToastProvider} from "@/contexts/ToastContext";
import {RecaptchaWrapper} from "@/components/RecaptchaWrapper";
import React from "react";

const geistSans = Geist({
    variable: "--font-geist-sans",
    subsets: ["latin"],
});

const geistMono = Geist_Mono({
    variable: "--font-geist-mono",
    subsets: ["latin"],
});

export const metadata: Metadata = {
    title: "CTU Connect - Mạng xã hội sinh viên Đại học Cần Thơ",
    description: "Kết nối sinh viên Đại học Cần Thơ, chia sẻ thông tin, học tập và giao lưu",
    keywords: "CTU, Can Tho University, sinh viên, mạng xã hội, học tập",
    authors: [{name: "CTU Connect Team"}],
};

export const viewport = {
    width: "device-width",
    initialScale: 1,
};

export default function RootLayout({
                                       children,
                                   }: {
    children: React.ReactNode;
}) {
    return (
        <html lang="vi">
        <body className={`${geistSans.variable} ${geistMono.variable} antialiased`}>
        <RecaptchaWrapper>
            <QueryProvider>
                <AuthProvider>
                    <ToastProvider>

                        {children}

                    </ToastProvider>
                </AuthProvider>
            </QueryProvider>
        </RecaptchaWrapper>
        </body>
        </html>
    );
}
