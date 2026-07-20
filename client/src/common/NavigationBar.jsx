import {useState} from 'react'
import {Link} from 'react-router-dom'
import {Menu, X, ChevronDown, ShoppingCart} from "lucide-react"

import {Button} from '@/components/ui/button.jsx'
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu.jsx"
import {APP_NAME, hasPermission} from '@/utils/index.js'
import {useUserStore} from '@/store/useUserStore.js'
import {useCartStore} from '@/store/useCartStore.js'
import { PERMISSION } from '@/utils/enums.js'
import { cn } from '@/lib/utils.js'

const BASE_MENU = [{name: 'Home', href: '/'}]
const LOGIN_MENU = [{name: 'Login', href: '/auth/login'}]
const PROFILE_MENU = [
    {name: 'Profile', href: '/profile'},
]

export default function Navbar() {
    const {user, logout} = useUserStore();
    const cartCount = useCartStore((state) => state.getCartCount());
    
    const isAuthenticated = !!user;

    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
    const [activeMenu, setActiveMenu] = useState(null);

    
    const getMenuItems = () => {
        return [
            ...BASE_MENU,

            {
                name: "Products",
                href: "/products",
                submenu: [
                    {name: "Product List", href: "/products"},
                    ...(hasPermission(user, PERMISSION.SUPER_ADMIN_ACCESS)
                        ? [{name: "Add Product", href: "/products/create"}]
                        : []),
                ],
            },

            ...(isAuthenticated
                ? [
                    {name: 'Orders', href: '/orders'},
                ]
                : []),

            ...(hasPermission(user, PERMISSION.ADMIN_ACCESS)
                ? [
                    {
                        name: "Stock",
                        href: "/stocks",
                        submenu: [
                            {name: "Stock List", href: "/stocks"},
                            {name: "Stock Item List", href: "/stocks/items"},
                            {name: "Add Stock", href: "/stocks/create"},
                        ],
                    },
                    {name: 'Users', href: '/users'}
                ]
                : []),

            ...(hasPermission(user, PERMISSION.SUPER_ADMIN_ACCESS)
                ? [
                    {name: "Roles", href: "/roles"},
                    {name: "Sales", href: "/sales"},
                ]
                : []),

            ...(isAuthenticated
                ? [
                    {name: "Chats", href: "/chats"},
                ]
                : []),
        ];
    };

    const finalMenuItems = getMenuItems();

    return (
        <nav className="w-full sticky top-0 z-50 transition-all duration-300 bg-white/80 backdrop-blur-md border-b border-slate-200/60">
            {/* pc menu */}
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex justify-between h-16 md:h-20">
                    <div className="flex w-full justify-between items-center">
                        <div className="flex-shrink-0 flex items-center group">
                            <Link to="/" className="flex items-center gap-2 transition-transform duration-300 active:scale-95">
                                <div className="relative">
                                    <div className="absolute -inset-1 bg-blue-600/20 rounded-full blur group-hover:bg-blue-600/30 transition-all" />
                                    <img src="/navbar/icon3.png" className="relative h-10 w-10 md:h-12 md:w-12 object-contain" alt="logo"/>
                                </div>
                                <p className="text-xl md:text-2xl font-black bg-clip-text text-transparent bg-gradient-to-r from-blue-700 to-indigo-800 tracking-tight">
                                    {APP_NAME}
                                </p>
                            </Link>
                        </div>

                        <div className="hidden sm:ml-6 sm:flex sm:space-x-1 lg:space-x-4 h-full items-center">
                            {finalMenuItems.map((item, index) => (
                                <div
                                    key={index}
                                    className="relative h-full flex items-center"
                                    onMouseEnter={() => setActiveMenu(index)}
                                    onMouseLeave={() => setActiveMenu(null)}
                                >
                                    <Link
                                        to={item.href}
                                        className={cn(
                                            "flex items-center gap-1.5 px-3 py-2 rounded-xl text-sm font-semibold transition-all duration-200",
                                            activeMenu == index 
                                                ? "bg-blue-50 text-blue-700" 
                                                : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                                        )}
                                    >
                                        {item.name}
                                        {item.submenu && <ChevronDown className={cn("h-3.5 w-3.5 transition-transform duration-200", activeMenu == index && "rotate-180")} />}
                                    </Link>

                                    {item.submenu && activeMenu == index && (
                                        <div
                                            className="absolute left-0 top-[calc(100%-8px)] z-50 min-w-[220px] rounded-2xl bg-white p-2 shadow-xl border border-slate-100 animate-in fade-in slide-in-from-top-2 duration-200">
                                            {item.submenu.map((subItem) => (
                                                <Link
                                                    key={subItem.name}
                                                    to={subItem.href}
                                                    className="block px-4 py-2.5 text-sm font-medium text-slate-600 hover:bg-blue-50 hover:text-blue-700 rounded-xl transition-colors"
                                                >
                                                    {subItem.name}
                                                </Link>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            ))}

                            <div className="h-6 w-px bg-slate-200 mx-2" />

                            <Link to="/cart" className="relative p-2.5 rounded-full text-slate-600 hover:bg-slate-50 hover:text-blue-600 transition-all active:scale-90 group">
                                <ShoppingCart className="h-5 w-5"/>
                                    {cartCount > 0 && (
                                        <span
                                            className="absolute top-1 right-1 bg-blue-600 text-white text-[10px] font-black h-4 w-4 flex items-center justify-center rounded-full shadow-lg shadow-blue-200 animate-in zoom-in duration-300">
                                            {cartCount}
                                        </span>
                                    )}
                            </Link>

                            <div className="flex items-center ml-2">
                                {isAuthenticated ?
                                    <DropdownMenu>
                                        <DropdownMenuTrigger asChild>
                                            <Button variant="ghost"
                                                    className="flex items-center gap-2 pl-2 pr-3 py-1.5 h-auto rounded-full border border-slate-200 hover:bg-slate-50 hover:border-blue-200 transition-all">
                                                <div className="w-7 h-7 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-[10px] font-bold text-white shadow-sm">
                                                    {user?.name?.slice(0, 1)}
                                                </div>
                                                <p className="text-sm font-bold text-slate-700">{user?.name?.split(' ')[0]}</p>
                                                <ChevronDown className="h-3 w-3 text-slate-400"/>
                                            </Button>
                                        </DropdownMenuTrigger>
                                        <DropdownMenuContent align="end" className="w-56 rounded-2xl p-2 shadow-xl border-slate-100">
                                            {PROFILE_MENU.map((menu, index) => (
                                                <DropdownMenuItem asChild className="rounded-xl cursor-pointer py-2.5" key={index}>
                                                    <Link to={menu.href}
                                                          className="text-sm font-semibold text-slate-700 flex items-center gap-2">
                                                        <span className="w-5 flex justify-center opacity-50">👤</span>
                                                        {menu.name}
                                                    </Link>
                                                </DropdownMenuItem>
                                            ))}
                                            <div className="my-1 border-t border-slate-50" />
                                            <DropdownMenuItem asChild
                                                              className="rounded-xl cursor-pointer py-2.5 text-red-600 focus:text-red-600 focus:bg-red-50">
                                                <button className="w-full flex items-center gap-2 font-semibold"
                                                      onClick={logout}>
                                                    <span className="w-5 flex justify-center opacity-50">🚪</span>
                                                    Logout
                                                </button>
                                            </DropdownMenuItem>
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                    :
                                    <Link to={LOGIN_MENU[0].href}
                                          className="inline-flex items-center px-6 py-2.5 text-sm font-bold text-white bg-blue-600 hover:bg-blue-700 rounded-full shadow-lg shadow-blue-200 transition-all hover:scale-[1.02] active:scale-95"
                                    >
                                        {LOGIN_MENU[0].name}
                                    </Link>
                                }
                            </div>
                        </div>
                    </div>

                    <div className="-mr-2 flex items-center sm:hidden">
                        <Button
                            variant="ghost"
                            size="icon"
                            className="rounded-xl text-slate-600 hover:bg-slate-50"
                            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                        >
                            {isMobileMenuOpen ? (
                                <X className="h-6 w-6" />
                            ) : (
                                <Menu className="h-6 w-6" />
                            )}
                        </Button>
                    </div>
                </div>
            </div>

            {/* Mobile menu */}
            <div className={cn(
                "sm:hidden bg-white border-t border-slate-100 transition-all duration-300 overflow-hidden",
                isMobileMenuOpen ? "max-h-[80vh] opacity-100" : "max-h-0 opacity-0"
            )}>
                <div className="space-y-1 p-4">
                    {finalMenuItems.map((item, index) => (
                        <div key={index}>
                            {item.submenu ? (
                                <div className="py-2">
                                    <p className="px-3 text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-1">{item.name}</p>
                                    <div className="space-y-1">
                                        {item.submenu.map((sub) => (
                                            <Link
                                                key={sub.name}
                                                to={sub.href}
                                                className="block px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 rounded-xl"
                                                onClick={() => setIsMobileMenuOpen(false)}
                                            >
                                                {sub.name}
                                            </Link>
                                        ))}
                                    </div>
                                </div>
                            ) : (
                                <Link
                                    to={item.href}
                                    className="block px-3 py-3 text-sm font-bold text-slate-700 hover:bg-slate-50 rounded-xl"
                                    onClick={() => setIsMobileMenuOpen(false)}
                                >
                                    {item.name}
                                </Link>
                            )}
                        </div>
                    ))}

                    <div className="pt-4 border-t border-slate-50 mt-4 space-y-3">
                        <Link 
                            to="/cart" 
                            className="flex items-center justify-between px-3 py-3 rounded-xl bg-slate-50 text-slate-700"
                            onClick={() => setIsMobileMenuOpen(false)}
                        >
                            <div className="flex items-center gap-3">
                                <ShoppingCart className="h-5 w-5"/>
                                <span className="font-bold">My Cart</span>
                            </div>
                            {cartCount > 0 && <span className="bg-blue-600 text-white text-xs font-bold px-2 py-0.5 rounded-full">{cartCount}</span>}
                        </Link>

                        {isAuthenticated ? (
                            <div className="space-y-2">
                                <Link 
                                    to="/profile" 
                                    className="block w-full text-center py-3 rounded-xl border border-slate-200 font-bold text-slate-700"
                                    onClick={() => setIsMobileMenuOpen(false)}
                                >
                                    My Profile
                                </Link>
                                <Button 
                                    variant="ghost" 
                                    className="w-full py-6 rounded-xl text-red-600 font-bold hover:bg-red-50 hover:text-red-700"
                                    onClick={() => {
                                        logout();
                                        setIsMobileMenuOpen(false);
                                    }}
                                >
                                    Logout
                                </Button>
                            </div>
                        ) : (
                            <Link 
                                to="/auth/login" 
                                className="block w-full text-center py-3 rounded-xl bg-blue-600 text-white font-bold"
                                onClick={() => setIsMobileMenuOpen(false)}
                            >
                                Login
                            </Link>
                        )}
                    </div>
                </div>
            </div>
        </nav>
    )
    }